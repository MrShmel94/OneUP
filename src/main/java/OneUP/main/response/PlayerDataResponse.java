package OneUP.main.response;

import java.util.Map;

public record PlayerDataResponse(
        Map<String, Map<String, String>> troops,
        Map<String, String> resources,
        Map<String, String> elixir
) {
}
