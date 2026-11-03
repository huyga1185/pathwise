package uk.huy.pathwise.core.apiresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T> (@Nullable String message, T data) {
    public ApiResponse (T data) {
        this(null, data);
    }
}
