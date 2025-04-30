package OneUP.main.exception;

public class UserNotValidatedException extends RuntimeException {
    public UserNotValidatedException() {
        super("User has not confirmed registration.");
    }
}
