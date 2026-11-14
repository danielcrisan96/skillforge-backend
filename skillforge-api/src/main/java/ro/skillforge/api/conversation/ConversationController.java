package ro.skillforge.api.conversation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import ro.skillforge.api.conversation.dto.ConversationResponse;
import ro.skillforge.api.conversation.dto.SaveConversationRequest;

import java.util.List;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static ro.skillforge.api.shared.web.ApiKeyFilter.USER_ID_HEADER;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Conversațiile utilizatorului, cea mai recent salvată prima")
    List<ConversationResponse> findAll(@RequestHeader(USER_ID_HEADER) String userId) {
        return conversationService.findAllFor(userId);
    }

    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Conversația, cu mesajele ei")
    @ApiResponse(responseCode = "404", description = "Utilizatorul nu are o conversație cu acest id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ConversationResponse findById(@RequestHeader(USER_ID_HEADER) String userId, @PathVariable String id) {
        return conversationService.findById(userId, id);
    }

    // PUT, nu POST: id-ul e ales de client, iar același instantaneu trimis de două ori lasă serverul ca după o
    // singură trimitere. Un client nesigur că cererea a ajuns o poate retrimite.
    @PutMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Salvată; răspunsul poartă versiunea de trimis la următoarea salvare")
    @ApiResponse(responseCode = "400", description = "Body invalid: titlu lipsă, rol necunoscut, prea multe mesaje",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Id-ul aparține conversației altui utilizator",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Versiunea trimisă nu mai e cea curentă: altă fereastră a salvat între timp",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ConversationResponse save(@RequestHeader(USER_ID_HEADER) String userId, @PathVariable String id,
                              @Valid @RequestBody SaveConversationRequest request) {
        return conversationService.save(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Ștearsă")
    @ApiResponse(responseCode = "404", description = "Utilizatorul nu are o conversație cu acest id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    void delete(@RequestHeader(USER_ID_HEADER) String userId, @PathVariable String id) {
        conversationService.delete(userId, id);
    }
}
