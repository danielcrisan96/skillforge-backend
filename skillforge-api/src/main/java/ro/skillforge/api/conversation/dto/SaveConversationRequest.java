package ro.skillforge.api.conversation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import ro.skillforge.api.conversation.Message;

import java.time.Instant;
import java.util.List;

// Instantaneul unei conversații, așa cum îl trimite aplicația web la finalul unui răspuns.
//
// version este versiunea pe care clientul a primit-o ultima dată de la server; lipsește la prima salvare.
// createdAt contează doar la prima salvare: după aceea rămâne cel din baza de date.
@Builder(toBuilder = true)
public record SaveConversationRequest(@NotBlank @Size(max = 200) String title,
                                      Instant createdAt,
                                      Long version,
                                      @NotNull @Size(max = 500) List<@Valid Message> messages) {
}
