package ro.skillforge.api.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O bucată dintr-un mesaj, în forma în care o ține aplicația web (UIMessage.parts). Păstrăm doar părțile de text:
// serverul Next.js le filtrează înainte să ajungă aici.
public record MessagePart(@NotBlank @Size(max = 30) String type,
                          @Size(max = 200_000) String text) {
}
