package {applicationName};

public class ConstraintViolationException extends RuntimeException {
    public ConstraintViolationException(String message) {
        super(message);
    }
    public ConstraintViolationException() {
        super();
    }
}
