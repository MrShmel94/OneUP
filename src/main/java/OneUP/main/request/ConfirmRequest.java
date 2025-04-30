package OneUP.main.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRequest(
        @NotBlank String nickname,
        @NotBlank String code
) {}
