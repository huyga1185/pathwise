package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import uk.huy.pathwise.core.phonenumber.ValidPhoneNumber;
import uk.huy.pathwise.fleet.model.CapacityUnit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Partial change of one stop ({@code PATCH /plans/{id}/stops/{stopId}}), with JSON Merge Patch semantics (RFC 7396):
 * <ul>
 *     <li>a field <b>absent</b> from the JSON keeps its value;</li>
 *     <li>a field present with {@code null} clears it: no time window, no demand, service time 0, no coordinates
 *     (the stop is geocoded again from its address);</li>
 *     <li>{@code label} cannot be cleared; {@code lat} and {@code lon} are sent together.</li>
 * </ul>
 * A class with setters rather than a record, because a record cannot tell an absent field from a {@code null} one:
 * Jackson calls a setter only for fields present in the JSON.
 */
@Getter
public class PatchStopRequest {
    @NotNull(message = "VERSION_REQUIRED")
    private Long version;

    @Size(max = 150, message = "INVALID_STOP_LABEL_LENGTH")
    private String label;

    @Size(max = 100, message = "INVALID_RECIPIENT_NAME_LENGTH")
    private String recipientName;

    @ValidPhoneNumber
    private String recipientPhone;

    @Size(max = 500, message = "INVALID_ADDRESS_LENGTH")
    private String address;

    @DecimalMin(value = "-90", message = "INVALID_LATITUDE")
    @DecimalMax(value = "90", message = "INVALID_LATITUDE")
    private Double lat;

    @DecimalMin(value = "-180", message = "INVALID_LONGITUDE")
    @DecimalMax(value = "180", message = "INVALID_LONGITUDE")
    private Double lon;

    private Map<CapacityUnit, @NotNull(message = "INVALID_DEMAND") @PositiveOrZero(message = "INVALID_DEMAND") Integer> demands;

    @PositiveOrZero(message = "INVALID_SERVICE_TIME")
    @Max(value = 86_400, message = "INVALID_SERVICE_TIME")
    private Integer serviceTimeSeconds;

    @Valid
    private TimeWindowRequest timeWindow;

    @Size(max = 500, message = "INVALID_NOTE_LENGTH")
    private String note;

    @Getter(lombok.AccessLevel.NONE)
    private final Set<String> present = new HashSet<>();

    /**
     * @return true if the field was in the JSON, even with {@code null}
     */
    public boolean has(String field) {
        return present.contains(field);
    }

    @AssertTrue(message = "STOP_LABEL_REQUIRED")
    public boolean isLabelKept() {
        return !has("label") || (label != null && !label.isBlank());
    }

    @AssertTrue(message = "COORDINATES_INCOMPLETE")
    public boolean isCoordinatesComplete() {
        return has("lat") == has("lon") && (lat == null) == (lon == null);
    }

    @AssertTrue(message = "ORDER_DEMAND_NOT_ALLOWED")
    public boolean isWithoutOrderDemand() {
        return demands == null || !demands.containsKey(CapacityUnit.ORDER);
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setLabel(String label) {
        this.label = label;
        present.add("label");
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
        present.add("recipientName");
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
        present.add("recipientPhone");
    }

    public void setAddress(String address) {
        this.address = address;
        present.add("address");
    }

    public void setLat(Double lat) {
        this.lat = lat;
        present.add("lat");
    }

    public void setLon(Double lon) {
        this.lon = lon;
        present.add("lon");
    }

    public void setDemands(Map<CapacityUnit, Integer> demands) {
        this.demands = demands;
        present.add("demands");
    }

    public void setServiceTimeSeconds(Integer serviceTimeSeconds) {
        this.serviceTimeSeconds = serviceTimeSeconds;
        present.add("serviceTimeSeconds");
    }

    public void setTimeWindow(TimeWindowRequest timeWindow) {
        this.timeWindow = timeWindow;
        present.add("timeWindow");
    }

    public void setNote(String note) {
        this.note = note;
        present.add("note");
    }
}
