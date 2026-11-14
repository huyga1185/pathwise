package uk.huy.pathwise.core.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    @Test
    void constraintOf_whenDuplicateEmail_returnTheConstraintNotTheValue() {
        SQLIntegrityConstraintViolationException sql =
                new SQLIntegrityConstraintViolationException("Duplicate entry 'chu@tiem.vn' for key 'uk_users_email'");

        String fromMessage = GlobalExceptionHandler.constraintOf(new DataIntegrityViolationException("x", sql));
        String fromHibernate = GlobalExceptionHandler.constraintOf(new DataIntegrityViolationException("x",
                new ConstraintViolationException("could not execute statement", sql, "uk_users_email")));

        assertThat(fromMessage).isEqualTo("uk_users_email");
        assertThat(fromHibernate).isEqualTo("uk_users_email");
    }

    @Test
    void constraintOf_whenMessageHasNoConstraint_returnTheExceptionType() {
        String result = GlobalExceptionHandler.constraintOf(new DataIntegrityViolationException("x",
                new SQLIntegrityConstraintViolationException("Column 'email' cannot be null: chu@tiem.vn")));

        assertThat(result).isEqualTo("SQLIntegrityConstraintViolationException").doesNotContain("@");
    }
}
