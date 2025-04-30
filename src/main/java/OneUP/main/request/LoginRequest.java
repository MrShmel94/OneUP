package OneUP.main.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String nickname,
        @NotBlank String password
) {}
