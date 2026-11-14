package uk.huy.pathwise.planning.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.planning.infrastructure.sheet.CsvSheetReader;
import uk.huy.pathwise.planning.infrastructure.sheet.ImportTemplateWriter;
import uk.huy.pathwise.planning.infrastructure.sheet.XlsxSheetReader;

@Configuration
@EnableConfigurationProperties(GeocodePlanProperties.class)
class PlanningConfig {
    @Bean
    CsvSheetReader csvSheetReader() {
        return new CsvSheetReader();
    }

    @Bean
    XlsxSheetReader xlsxSheetReader() {
        return new XlsxSheetReader();
    }

    @Bean
    ImportTemplateWriter importTemplateWriter() {
        return new ImportTemplateWriter();
    }
}
