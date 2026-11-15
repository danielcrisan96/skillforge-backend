package ro.skillforge.api.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Lasă să treacă doar cererile care vin de la serverul aplicației Next.js: cele cu cheia dintre servicii în
 * {@code X-Api-Key}.
 *
 * Browserul nu vorbește niciodată cu acest serviciu. Sesiunea utilizatorului e verificată în Next.js (Auth.js), iar
 * de acolo cererea pleacă mai departe cu id-ul utilizatorului în {@code X-User-Id}. Cheia e ce ne dă voie să credem
 * acel header: cine nu o are nu poate pretinde că e alt utilizator.
 *
 * Fără cheie configurată, serviciul rămâne ÎNCHIS (503), nu deschis: o configurare uitată nu trebuie să însemne
 * date citibile de oricine.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    public static final String USER_ID_HEADER = "X-User-Id";

    // Sănătatea e întrebată de Docker și de Cloud Run, care nu au cheia; documentația nu conține date.
    // Doar /actuator/health, nu tot /actuator/: info și metrics nu sunt date ale utilizatorilor, dar spun ceva
    // despre serviciu (versiuni, memorie, numărul de cereri), iar pe Cloud Run adresa e publică.
    private static final List<String> OPEN_PATHS = List.of("/actuator/health", "/swagger-ui", "/v3/api-docs");

    private final String apiKey;

    public ApiKeyFilter(@Value("${skillforge.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return OPEN_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (apiKey.isBlank()) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "Cheia dintre servicii nu e configurată pe server.");
            return;
        }

        String sentKey = request.getHeader(API_KEY_HEADER);
        // MessageDigest.isEqual compară în timp constant: durata răspunsului nu spune câte caractere au fost corecte.
        if (sentKey == null || !MessageDigest.isEqual(sentKey.getBytes(UTF_8), apiKey.getBytes(UTF_8))) {
            reject(response, HttpStatus.UNAUTHORIZED, "Lipsește cheia dintre servicii sau nu e cea corectă.");
            return;
        }

        chain.doFilter(request, response);
    }

    // Filtrul rulează înaintea controllerelor, deci GlobalExceptionHandler nu ajunge aici: răspunsul se scrie de mână,
    // în aceeași formă ProblemDetail ca restul erorilor.
    private static void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}"
                .formatted(status.getReasonPhrase(), status.value(), detail));
    }
}
