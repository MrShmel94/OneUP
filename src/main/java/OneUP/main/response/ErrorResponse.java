package OneUP.main.response;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message
) {}
