package OneUP.main.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid nickname or password.");
    }
}
