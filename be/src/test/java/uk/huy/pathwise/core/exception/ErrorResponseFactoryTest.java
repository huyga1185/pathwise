package uk.huy.pathwise.core.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseFactoryTest {
    @Test
    void body_whenProfileIsNotDev_omitDebugFields() {
        ErrorResponseFactory factory = new ErrorResponseFactory(new MockEnvironment());

        ErrorResponse body = factory.body(ErrorCode.INTERNAL_ERROR, null, new IllegalStateException("boom"));

        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.exception()).isNull();
        assertThat(body.stackTrace()).isNull();
    }

    @Test
    void body_whenProfileIsDev_includeExceptionAndStackTrace() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        ErrorResponseFactory factory = new ErrorResponseFactory(env);

        ErrorResponse body = factory.body(ErrorCode.INTERNAL_ERROR, null, new IllegalStateException("boom"));

        assertThat(body.exception()).isEqualTo("java.lang.IllegalStateException: boom");
        assertThat(body.stackTrace()).isNotEmpty().hasSizeLessThanOrEqualTo(10);
    }

    @Test
    void body_whenNoException_omitDebugFieldsEvenInDev() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        ErrorResponse body = new ErrorResponseFactory(env).body(ErrorCode.EMAIL_EXISTED, null, null);

        assertThat(body.exception()).isNull();
        assertThat(body.stackTrace()).isNull();
    }
}
