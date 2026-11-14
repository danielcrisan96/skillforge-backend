package ro.skillforge.api.shared.error;

/**
 * Clasa de bază pentru „lucrul cerut nu există”. {@link GlobalExceptionHandler} răspunde cu 404.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
