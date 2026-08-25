package uk.huy.pathwise.core.apiresponse;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T> (String message, T data) {
    public ApiResponse (T data) {
        this(null, data);
    }
}
