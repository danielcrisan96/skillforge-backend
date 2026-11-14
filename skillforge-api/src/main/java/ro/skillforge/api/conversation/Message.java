package ro.skillforge.api.conversation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Un mesaj al conversației, încorporat în documentul ei. Metadatele lipsesc la mesajele utilizatorului.
public record Message(@NotBlank @Size(max = 100) String id,
                      @NotBlank @Pattern(regexp = "user|assistant|system") String role,
                      @NotNull @Size(max = 50) List<@Valid MessagePart> parts,
                      @Valid MessageMetadata metadata) {
}
