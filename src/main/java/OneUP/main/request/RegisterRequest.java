package OneUP.main.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String nickname,
        String fullName,
        @NotBlank String timezone,
        @NotBlank String password,
        String location,
        String about
) {}