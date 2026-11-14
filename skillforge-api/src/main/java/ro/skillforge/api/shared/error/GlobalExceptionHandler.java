package ro.skillforge.api.shared.error;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Transformă excepțiile aplicației în răspunsuri {@link ProblemDetail}, într-un singur loc, astfel încât
 * controllerele descriu doar drumul fericit. Erorile framework-ului (body invalid, validare eșuată, header lipsă)
 * le tratează Spring Boot, prin {@code spring.mvc.problemdetails.enabled}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
    }

    // Versiunea trimisă de client nu mai e cea din baza de date: altcineva a salvat între timp.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(CONFLICT,
                "Conversația a fost modificată între timp din altă fereastră. Reîncarcă pentru versiunea curentă.");
    }

    // Clientul a trimis conversația ca nouă (fără versiune), dar id-ul există deja.
    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(DuplicateKeyException exception) {
        return ProblemDetail.forStatusAndDetail(CONFLICT,
                "Există deja o conversație cu acest id. Reîncarcă pentru versiunea curentă.");
    }
}
