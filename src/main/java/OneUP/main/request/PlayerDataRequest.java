package OneUP.main.request;

import java.util.Map;

public record PlayerDataRequest(
        String nickname,
        Map<String, Map<String, String>> troops,
        Map<String, String> resources,
        Map<String, String> elixir
) {}