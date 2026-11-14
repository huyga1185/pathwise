package uk.huy.pathwise.planning;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T61 acceptance: {@code POST /plans/import} reads a file into stops and saves nothing (P-67, P-68).
 */
@AutoConfigureMockMvc
class PlanImportIntegrationTest extends AbstractIntegrationTest {
    private static final String IMPORT = "/api/v1/plans/import";
    private static final String HEADER = "Nhãn / mã đơn,Người nhận,Số điện thoại,Địa chỉ,Vĩ độ,Kinh độ,KG,Thùng,Kiện,"
            + "Phút phục vụ,Giao từ,Giao đến,Ghi chú";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUsers users;
    private TestStore store;

    @BeforeEach
    void setUp() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        store = users.createStore();
    }

    @Test
    void importStops_whenCsvWithGoodAndBadRows_returnEveryRowWithFieldErrorsAndSaveNothing() throws Exception {
        long plans = count("route_plans");
        long stops = count("route_plan_stops");
        String csv = HEADER + ",Cột lạ\n"
                + "ĐH-1,Chị Lan,0905 123 456,\"12 Lê Duẩn, Đà Nẵng\",,,,2,,5,9:00,11h30,Gọi trước,x\n"
                + ",,,Không nhãn,,,,,,,,,,\n"
                + "ĐH-3,,123,,16.06,abc,-1,1.5,,2000,25:00,10:00,,\n"
                + "ĐH-4,,,,\"16,06\",108.2,,,,,,,,\n";

        importFile(store.dispatcher(), "Điểm giao.CSV", csv.getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validCount").value(2))
                .andExpect(jsonPath("$.data.errorCount").value(2))
                .andExpect(jsonPath("$.data.ignoredColumns[0]").value("Cột lạ"))
                .andExpect(jsonPath("$.data.rows", hasSize(4)))
                .andExpect(jsonPath("$.data.rows[0].row").value(2))
                .andExpect(jsonPath("$.data.rows[0].errors", hasSize(0)))
                .andExpect(jsonPath("$.data.rows[0].stop.label").value("ĐH-1"))
                .andExpect(jsonPath("$.data.rows[0].stop.recipientPhone").value("+84905123456"))
                .andExpect(jsonPath("$.data.rows[0].stop.address").value("12 Lê Duẩn, Đà Nẵng"))
                .andExpect(jsonPath("$.data.rows[0].stop.demands.BOX").value(2))
                .andExpect(jsonPath("$.data.rows[0].stop.serviceTimeSeconds").value(300))
                .andExpect(jsonPath("$.data.rows[0].stop.timeWindow.start").value("09:00"))
                .andExpect(jsonPath("$.data.rows[0].stop.timeWindow.end").value("11:30"))
                .andExpect(jsonPath("$.data.rows[1].row").value(3))
                .andExpect(jsonPath("$.data.rows[1].errors[0].field").value("label"))
                .andExpect(jsonPath("$.data.rows[1].errors[0].code").value("STOP_LABEL_REQUIRED"))
                .andExpect(jsonPath("$.data.rows[2].stop.lat").value(16.06))
                .andExpect(jsonPath("$.data.rows[2].stop.recipientPhone").value("123"))
                .andExpect(jsonPath("$.data.rows[3].errors", hasSize(0)))
                .andExpect(jsonPath("$.data.rows[3].stop.lat").value(16.06));
        assertThat(count("route_plans")).isEqualTo(plans);
        assertThat(count("route_plan_stops")).isEqualTo(stops);
    }

    @Test
    void importStops_whenRowHasBadValues_reportOneErrorPerField() throws Exception {
        String csv = HEADER + "\nĐH-3,,123,,16.06,abc,-1,1.5,,2000,25:00,10:00,\n";

        String body = importFile(store.owner(), "a.csv", csv.getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(errorsOf(body)).containsExactlyInAnyOrder(
                "lon:INVALID_LONGITUDE", "demands:INVALID_DEMAND", "serviceTimeSeconds:INVALID_SERVICE_TIME",
                "timeWindow.start:INVALID_TIME_FORMAT", "recipientPhone:PHONE_NUMBER_WRONG_REGION");
    }

    @Test
    void importStops_whenXlsxWithTypedCells_readNumbersAndTimes() throws Exception {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            CellStyle time = workbook.createCellStyle();
            time.setDataFormat(workbook.createDataFormat().getFormat("hh:mm"));
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("SĐT");
            header.createCell(1).setCellValue("nhan");
            header.createCell(2).setCellValue("Vi do");
            header.createCell(3).setCellValue("KINH ĐỘ");
            header.createCell(4).setCellValue("Giao từ");
            header.createCell(5).setCellValue("Giao đến");
            header.createCell(6).setCellValue("Kiện");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(905123456d);
            row.createCell(1).setCellValue("ĐH-9");
            row.createCell(2).setCellValue(16.0605);
            row.createCell(3).setCellValue(108.1985);
            row.createCell(4).setCellValue(8.0 / 24);
            row.getCell(4).setCellStyle(time);
            row.createCell(5).setCellValue(10.5 / 24);
            row.getCell(5).setCellStyle(time);
            row.createCell(6).setCellValue(3d);
            workbook.write(out);
            file = out.toByteArray();
        }

        importFile(store.owner(), "diem-giao.xlsx", file)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validCount").value(1))
                .andExpect(jsonPath("$.data.rows[0].stop.label").value("ĐH-9"))
                .andExpect(jsonPath("$.data.rows[0].stop.recipientPhone").value("+84905123456"))
                .andExpect(jsonPath("$.data.rows[0].stop.lat").value(16.0605))
                .andExpect(jsonPath("$.data.rows[0].stop.lon").value(108.1985))
                .andExpect(jsonPath("$.data.rows[0].stop.demands.PACKAGE").value(3))
                .andExpect(jsonPath("$.data.rows[0].stop.timeWindow.start").value("08:00"))
                .andExpect(jsonPath("$.data.rows[0].stop.timeWindow.end").value("10:30"));
    }

    @Test
    void importStops_whenTemplateDownloaded_itImportsAsOneValidRow() throws Exception {
        byte[] xlsx = mockMvc.perform(get(IMPORT + "/template.xlsx").header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("pathwise-mau-diem-giao.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        byte[] csv = mockMvc.perform(get(IMPORT + "/template.csv").header(HttpHeaders.AUTHORIZATION, store.dispatcher().bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(csv).startsWith(0xEF, 0xBB, 0xBF);

        for (String name : new String[]{"mau.xlsx", "mau.csv"}) {
            importFile(store.owner(), name, name.endsWith("xlsx") ? xlsx : csv)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.validCount").value(1))
                    .andExpect(jsonPath("$.data.ignoredColumns", hasSize(0)))
                    .andExpect(jsonPath("$.data.rows[0].stop.recipientPhone").value("+84905123456"))
                    .andExpect(jsonPath("$.data.rows[0].stop.demands.BOX").value(2))
                    .andExpect(jsonPath("$.data.rows[0].stop.timeWindow.end").value("11:00"));
        }
    }

    @Test
    void importStops_whenFileCannotBeUsed_return422Or413() throws Exception {
        expectInvalid(importFile(store.owner(), "a.xls", new byte[]{1}), "UNSUPPORTED_FILE_TYPE");
        expectInvalid(importFile(store.owner(), "a.xlsx", "Nhãn\nA".getBytes(StandardCharsets.UTF_8)), "UNREADABLE_FILE");
        expectInvalid(importFile(store.owner(), "a.csv", "Nhãn\n".getBytes(StandardCharsets.UTF_8)), "NO_ROWS");
        expectInvalid(importFile(store.owner(), "a.csv", "Địa chỉ\nĐà Nẵng".getBytes(StandardCharsets.UTF_8)),
                "LABEL_COLUMN_REQUIRED");

        StringBuilder tooMany = new StringBuilder("Nhãn\n");
        for (int i = 1; i <= 101; i++) tooMany.append("ĐH-").append(i).append('\n');
        importFile(store.owner(), "a.csv", tooMany.toString().getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_TOO_LARGE"));

        importFile(store.owner(), "a.csv", new byte[1024 * 1024 + 1])
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("IMPORT_FILE_TOO_LARGE"));
        mockMvc.perform(multipart(IMPORT).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        // Not multipart at all.
        mockMvc.perform(post(IMPORT).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void importStops_whenDriverOrAdminOrStoreLocked_refuse() throws Exception {
        byte[] csv = "Nhãn\nA".getBytes(StandardCharsets.UTF_8);
        importFile(store.driver(), "a.csv", csv).andExpect(status().isForbidden());
        importFile(users.createAdmin(), "a.csv", csv).andExpect(status().isForbidden());
        mockMvc.perform(get(IMPORT + "/template.csv").header(HttpHeaders.AUTHORIZATION, store.driver().bearer()))
                .andExpect(status().isForbidden());

        jdbcTemplate.update("UPDATE stores SET status = 'READ_ONLY', lock_reason = 'Kiểm thử', locked_at = NOW(6) WHERE id = ?",
                store.id());
        importFile(store.owner(), "a.csv", csv)
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.code").value("STORE_READ_ONLY"));
        mockMvc.perform(get(IMPORT + "/template.xlsx").header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isOk());
    }

    private ResultActions importFile(TestUser caller, String filename, byte[] content) throws Exception {
        return mockMvc.perform(multipart(IMPORT)
                .file(new MockMultipartFile("file", filename, "application/octet-stream", content))
                .header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private static void expectInvalid(ResultActions result, String code) throws Exception {
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_IMPORT_FILE"))
                .andExpect(jsonPath("$.details[0].field").value("file"))
                .andExpect(jsonPath("$.details[0].code").value(code));
    }

    private static List<String> errorsOf(String body) {
        List<Map<String, String>> errors = JsonPath.read(body, "$.data.rows[0].errors");
        return errors.stream().map(error -> error.get("field") + ":" + error.get("code")).toList();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
