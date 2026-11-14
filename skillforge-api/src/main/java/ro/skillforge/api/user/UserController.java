package ro.skillforge.api.user;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import ro.skillforge.api.user.dto.ProfileRequest;
import ro.skillforge.api.user.dto.ProfileResponse;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static ro.skillforge.api.shared.web.ApiKeyFilter.USER_ID_HEADER;

// „me” este utilizatorul din headerul X-User-Id, pus de serverul Next.js după ce a verificat sesiunea. Id-ul nu
// apare în cale: așa, un client nu poate cere datele altcuiva schimbând un segment din URL.
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    @ApiResponse(responseCode = "200", description = "Profilul salvat")
    @ApiResponse(responseCode = "404", description = "Utilizatorul nu are încă un profil salvat",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProfileResponse findProfile(@RequestHeader(USER_ID_HEADER) String userId) {
        return userService.findProfile(userId);
    }

    @PutMapping("/profile")
    @ApiResponse(responseCode = "200", description = "Profilul a fost salvat; prima salvare creează utilizatorul")
    @ApiResponse(responseCode = "400", description = "Body invalid: un câmp prea lung sau un nivel necunoscut",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ProfileResponse saveProfile(@RequestHeader(USER_ID_HEADER) String userId,
                                @Valid @RequestBody ProfileRequest request) {
        return userService.saveProfile(userId, request);
    }

    @DeleteMapping
    @ResponseStatus(NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Profilul și toate conversațiile utilizatorului au fost șterse")
    void delete(@RequestHeader(USER_ID_HEADER) String userId) {
        userService.delete(userId);
    }
}
