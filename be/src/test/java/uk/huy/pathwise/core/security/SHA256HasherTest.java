package uk.huy.pathwise.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.core.security.SHA256Hasher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SHA256HasherTest {
    private SHA256Hasher sha256Hasher;

    @BeforeEach
    void setUp() {
        this.sha256Hasher = new SHA256Hasher();
    }

    @Test
    void match_whenHashedStringOrRawStringIsNull_returnFalse() {
        boolean isFalse = sha256Hasher.match(null, null);

        assertThat(isFalse).isFalse();
    }

    @Test
    void match_whenHashedStringIsNull_returnFalse() {
        boolean isFalse = sha256Hasher.match(null, "not null");

        assertThat(isFalse).isFalse();
    }

    @Test
    void match_whenRawStringIsNull_returnFalse() {
        boolean isFalse = sha256Hasher.match("not null", null);

        assertThat(isFalse).isFalse();
    }
}
