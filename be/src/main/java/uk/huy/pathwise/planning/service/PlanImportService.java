package uk.huy.pathwise.planning.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.DetailedAppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.FieldErrorDetail;
import uk.huy.pathwise.core.phonenumber.PhoneNumberProcessor;
import uk.huy.pathwise.core.ratelimit.RateLimitDecision;
import uk.huy.pathwise.core.ratelimit.RateLimitedException;
import uk.huy.pathwise.core.ratelimit.RateLimiter;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.planning.dto.request.PlanStopRequest;
import uk.huy.pathwise.planning.dto.request.TimeWindowRequest;
import uk.huy.pathwise.planning.dto.response.ImportedRowResponse;
import uk.huy.pathwise.planning.dto.response.ImportedStopResponse;
import uk.huy.pathwise.planning.dto.response.PlanImportResponse;
import uk.huy.pathwise.planning.dto.response.TimeWindowResponse;
import uk.huy.pathwise.planning.infrastructure.sheet.CsvSheetReader;
import uk.huy.pathwise.planning.infrastructure.sheet.ImportTemplateWriter;
import uk.huy.pathwise.planning.infrastructure.sheet.SheetCell;
import uk.huy.pathwise.planning.infrastructure.sheet.SheetFormatException;
import uk.huy.pathwise.planning.infrastructure.sheet.SheetReader;
import uk.huy.pathwise.planning.infrastructure.sheet.SheetRow;
import uk.huy.pathwise.planning.infrastructure.sheet.XlsxSheetReader;
import uk.huy.pathwise.planning.model.ImportColumn;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a stop import file ({@code POST /plans/import}, T61, P-67, P-68) and saves nothing: each row comes back as a
 * stop of {@code PlanDraftRequest} with the errors the draft would get, so the client adds the good rows to its form
 * and the usual save validates and geocodes them. The file content is never logged (names, phone numbers).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanImportService {
    /** P-68. Same as {@code spring.servlet.multipart.max-file-size}, checked again here. */
    static final long MAX_FILE_BYTES = 1024 * 1024;
    static final String RATE_LIMIT_RULE = "plan-import-user";
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})\\s*[:hH]\\s*(\\d{2})");
    private static final Set<String> LOCATION_CODES = Set.of("COORDINATES_INCOMPLETE", "ADDRESS_OR_COORDINATES_REQUIRED");
    private static final Map<ImportColumn, CapacityUnit> DEMAND_COLUMNS = Map.of(
            ImportColumn.KG, CapacityUnit.KG, ImportColumn.BOX, CapacityUnit.BOX, ImportColumn.PACKAGE, CapacityUnit.PACKAGE);

    private final CsvSheetReader csvReader;
    private final XlsxSheetReader xlsxReader;
    private final ImportTemplateWriter templateWriter;
    private final Validator validator;
    private final PhoneNumberProcessor phoneNumberProcessor;
    private final RateLimiter rateLimiter;

    /**
     * @param filename the name of the uploaded file: {@code .xlsx} or {@code .csv}
     * @throws AppException {@code RATE_LIMITED} per user, {@code IMPORT_FILE_TOO_LARGE} over 1 MB, {@code PLAN_TOO_LARGE}
     *                      over 100 rows, {@code INVALID_IMPORT_FILE} with {@code file}: {@code UNSUPPORTED_FILE_TYPE},
     *                      {@code UNREADABLE_FILE}, {@code NO_ROWS}, or {@code LABEL_COLUMN_REQUIRED}
     */
    public PlanImportResponse importStops(UserIdentity actor, @Nullable String filename, byte[] content) {
        if (actor == null || actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        enforceRateLimit(actor.id());
        if (content.length > MAX_FILE_BYTES) throw new AppException(ErrorCode.IMPORT_FILE_TOO_LARGE);
        SheetReader reader = readerFor(filename);
        List<SheetRow> sheet;
        try {
            // Header + one row more than allowed, to tell "too many" without reading the rest.
            sheet = reader.read(content, PlanService.MAX_STOPS + 2);
        } catch (SheetFormatException e) {
            log.info("Import file refused: {}", e.getMessage());
            throw invalidFile("UNREADABLE_FILE");
        }
        if (sheet.size() < 2) throw invalidFile("NO_ROWS");
        if (sheet.size() - 1 > PlanService.MAX_STOPS) throw new AppException(ErrorCode.PLAN_TOO_LARGE);

        Map<ImportColumn, Integer> columns = new EnumMap<>(ImportColumn.class);
        List<String> ignored = new ArrayList<>();
        List<SheetCell> header = sheet.getFirst().cells();
        for (int i = 0; i < header.size(); i++) {
            String text = header.get(i).text();
            if (text.isEmpty()) continue;
            Optional<ImportColumn> column = ImportColumn.match(text);
            if (column.isPresent() && !columns.containsKey(column.get())) columns.put(column.get(), i);
            else ignored.add(text);
        }
        if (!columns.containsKey(ImportColumn.LABEL)) throw invalidFile("LABEL_COLUMN_REQUIRED");

        List<ImportedRowResponse> rows = sheet.subList(1, sheet.size()).stream()
                .map(row -> new RowParser(row, columns).parse())
                .toList();
        int errorCount = (int) rows.stream().filter(row -> !row.errors().isEmpty()).count();
        return new PlanImportResponse(rows, rows.size() - errorCount, errorCount, List.copyOf(ignored));
    }

    public byte[] xlsxTemplate() {
        return templateWriter.xlsx();
    }

    public byte[] csvTemplate() {
        return templateWriter.csv();
    }

    private SheetReader readerFor(@Nullable String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT).strip();
        if (name.endsWith(".xlsx")) return xlsxReader;
        if (name.endsWith(".csv")) return csvReader;
        throw invalidFile("UNSUPPORTED_FILE_TYPE");
    }

    private static DetailedAppException invalidFile(String code) {
        return new DetailedAppException(ErrorCode.INVALID_IMPORT_FILE, List.of(new FieldErrorDetail("file", code)));
    }

    private void enforceRateLimit(long userId) {
        RateLimitDecision decision;
        try {
            decision = rateLimiter.tryAcquire(RATE_LIMIT_RULE, String.valueOf(userId));
        } catch (StateStoreException e) {
            log.error("Rate limit could not be checked, refusing the request", e);
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        switch (decision) {
            case RateLimitDecision.Allowed allowed -> {}
            case RateLimitDecision.Limited limited -> throw new RateLimitedException(limited.retryAfter());
        }
    }

    /**
     * One row: cells to typed values (a cell that is not a number or a time is an error on its field), then the
     * validation of {@link PlanStopRequest}, keeping one error per field.
     */
    private final class RowParser {
        private final SheetRow row;
        private final Map<ImportColumn, Integer> columns;
        private final Map<String, String> errors = new LinkedHashMap<>();

        RowParser(SheetRow row, Map<ImportColumn, Integer> columns) {
            this.row = row;
            this.columns = columns;
        }

        ImportedRowResponse parse() {
            String label = text(ImportColumn.LABEL);
            String recipientName = text(ImportColumn.RECIPIENT_NAME);
            String recipientPhone = text(ImportColumn.RECIPIENT_PHONE);
            String address = text(ImportColumn.ADDRESS);
            Double lat = decimal(ImportColumn.LAT, "lat", "INVALID_LATITUDE");
            Double lon = decimal(ImportColumn.LON, "lon", "INVALID_LONGITUDE");
            Map<CapacityUnit, Integer> demands = new EnumMap<>(CapacityUnit.class);
            DEMAND_COLUMNS.forEach((column, unit) -> {
                Long quantity = whole(column, "demands", "INVALID_DEMAND");
                if (quantity == null) return;
                if (quantity < 0 || quantity > Integer.MAX_VALUE) errors.putIfAbsent("demands", "INVALID_DEMAND");
                else demands.put(unit, quantity.intValue());
            });
            Long minutes = whole(ImportColumn.SERVICE_MINUTES, "serviceTimeSeconds", "INVALID_SERVICE_TIME");
            Integer serviceTimeSeconds = null;
            if (minutes != null && (minutes < 0 || minutes > 24 * 60))
                errors.putIfAbsent("serviceTimeSeconds", "INVALID_SERVICE_TIME");
            else if (minutes != null) serviceTimeSeconds = (int) (minutes * 60);
            LocalTime start = time(ImportColumn.WINDOW_START, "timeWindow.start");
            LocalTime end = time(ImportColumn.WINDOW_END, "timeWindow.end");
            boolean timeError = errors.containsKey("timeWindow.start") || errors.containsKey("timeWindow.end");
            TimeWindowRequest timeWindow = timeError || (start == null && end == null) ? null : new TimeWindowRequest(start, end);
            String note = text(ImportColumn.NOTE);

            PlanStopRequest stop = new PlanStopRequest(null, label, recipientName, recipientPhone, address, lat, lon,
                    demands.isEmpty() ? null : demands, serviceTimeSeconds, timeWindow, note);
            boolean locationError = errors.containsKey("lat") || errors.containsKey("lon");
            for (ConstraintViolation<PlanStopRequest> violation : validator.validate(stop)) {
                String field = fieldOf(violation);
                if (locationError && LOCATION_CODES.contains(violation.getMessage())) continue;
                errors.putIfAbsent(field, violation.getMessage());
            }

            String phone = recipientPhone != null && !errors.containsKey("recipientPhone")
                    ? phoneNumberProcessor.toE164(recipientPhone) : recipientPhone;
            ImportedStopResponse imported = new ImportedStopResponse(label, recipientName, phone, address, lat, lon,
                    demands, serviceTimeSeconds, timeWindow == null ? null : new TimeWindowResponse(start, end), note);
            List<FieldErrorDetail> details = errors.entrySet().stream()
                    .map(entry -> new FieldErrorDetail(entry.getKey(), entry.getValue()))
                    .toList();
            return new ImportedRowResponse(row.number(), imported, details);
        }

        private SheetCell cell(ImportColumn column) {
            Integer index = columns.get(column);
            return index == null ? SheetCell.BLANK : row.cell(index);
        }

        private @Nullable String text(ImportColumn column) {
            String text = cell(column).text();
            return text.isEmpty() ? null : text;
        }

        private @Nullable Double decimal(ImportColumn column, String field, String code) {
            SheetCell cell = cell(column);
            if (cell.number() != null) return cell.number();
            if (cell.text().isEmpty()) return null;
            BigDecimal value = parseNumber(cell.text());
            if (value == null) {
                errors.putIfAbsent(field, code);
                return null;
            }
            return value.doubleValue();
        }

        /**
         * @return a whole number, or null when blank; an error on {@code field} when not a whole number
         */
        private @Nullable Long whole(ImportColumn column, String field, String code) {
            SheetCell cell = cell(column);
            if (cell.isBlank()) return null;
            BigDecimal value = cell.number() != null ? BigDecimal.valueOf(cell.number()) : parseNumber(cell.text());
            try {
                if (value != null) return value.longValueExact();
            } catch (ArithmeticException e) {
                // Not a whole number, or too big.
            }
            errors.putIfAbsent(field, code);
            return null;
        }

        private @Nullable LocalTime time(ImportColumn column, String field) {
            SheetCell cell = cell(column);
            if (cell.time() != null) return cell.time().withSecond(0).withNano(0);
            if (cell.text().isEmpty()) return null;
            Matcher matcher = TIME.matcher(cell.text());
            if (matcher.matches()) {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = Integer.parseInt(matcher.group(2));
                if (hour < 24 && minute < 60) return LocalTime.of(hour, minute);
            }
            errors.putIfAbsent(field, "INVALID_TIME_FORMAT");
            return null;
        }
    }

    /**
     * {@code 16.0605}, or {@code 16,0605} as a comma-decimal locale writes it; null if not a number.
     */
    static @Nullable BigDecimal parseNumber(String text) {
        String value = text.strip();
        if (value.indexOf('.') < 0 && value.indexOf(',') == value.lastIndexOf(',')) value = value.replace(',', '.');
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * {@code timeWindow.end}, {@code demands} for {@code demands[BOX].<map value>}.
     */
    private static String fieldOf(ConstraintViolation<?> violation) {
        return violation.getPropertyPath().toString()
                .replaceAll("\\.<[^>]+>", "")
                .replaceAll("\\[[^]]*]", "");
    }
}
