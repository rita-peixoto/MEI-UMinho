package Service.Exceptions;

public class InvalidDestination extends RuntimeException {


    public InvalidDestination() {
        super();
    }

    public InvalidDestination(String message) {
        super(message);
    }

    public InvalidDestination(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidDestination(Throwable cause) {
        super(cause);
    }

    protected InvalidDestination(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
