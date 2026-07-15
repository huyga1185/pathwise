package uk.huy.pathwise.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class UserModificationRequest {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<String> phoneNumber;
}
