package uk.huy.pathwise.core.search;

import java.util.Locale;

/**
 * Patterns for {@code LOWER(column) LIKE :pattern ESCAPE '\'} (T55): the user's text is matched literally, so
 * {@code %} and {@code _} in a search do not act as wildcards.
 */
public final class LikePattern {
    private LikePattern() {
    }

    /**
     * @return {@code %text%} in lower case with {@code \}, {@code %} and {@code _} escaped
     */
    public static String contains(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
