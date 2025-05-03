package OneUP.main.request;

import jakarta.validation.constraints.NotBlank;

public record ToggleBanRequest(
        @NotBlank String nickname,
        @NotBlank Boolean isBanned
) {
}
