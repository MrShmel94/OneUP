package OneUP.main.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
        @NotBlank String nickname,
        @NotBlank String role
) {
}
