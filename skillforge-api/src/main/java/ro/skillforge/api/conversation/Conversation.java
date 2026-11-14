package ro.skillforge.api.conversation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// Un document în colecția conversations, cu mesajele ÎNCORPORATE și cu utilizatorul REFERIT prin userId.
//
// Mesajele stau înăuntru pentru că se citesc mereu împreună cu conversația, iar aplicația web le salvează oricum
// ca instantaneu, o singură dată, la finalul răspunsului: o conversație este o singură scriere, atomică.
// Conversațiile stau în colecția lor pentru că un utilizator poate avea oricâte; încorporate în documentul lui,
// l-ar face să crească fără limită.
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@FieldNameConstants
@Document(collection = "conversations")
public class Conversation {
    // Generat de aplicația web (crypto.randomUUID), înainte de primul mesaj.
    @Id
    private String id;

    // Optimistic locking: salvarea reușește doar dacă versiunea din baza de date e încă cea citită de client.
    // Long, nu long: null înseamnă „document nou” și devine o inserare; orice altă valoare devine o actualizare
    // condiționată. Spring Data o mărește la fiecare salvare.
    @Version
    private Long version;

    // Câmpul după care se caută mereu: lista unui utilizator, raportul lui de consum, ștergerea contului.
    @Indexed
    @NotBlank
    private String userId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant updatedAt;

    @NotNull
    @Size(max = 500)
    private List<@Valid Message> messages;
}
