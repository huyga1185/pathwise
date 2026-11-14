package uk.huy.pathwise.planning.infrastructure.sheet;

/**
 * The file cannot be read as a sheet: not a valid file of its format, or too big once uncompressed. The message never
 * holds cell content.
 */
public class SheetFormatException extends RuntimeException {
    public SheetFormatException(String message) {
        super(message);
    }

    public SheetFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
