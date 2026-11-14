package ro.skillforge.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Un document în colecția users, cu profilul încorporat.
//
// Id-ul NU e generat de noi: e id-ul stabil pe care îl construiește Auth.js în aplicația web
// („github:<id numeric>”). Emailul, numele și poza contului nu se salvează aici: sunt datele furnizorului de
// autentificare, nu ale profilului, și nu avem nevoie de ele ca să servim conversațiile.
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@FieldNameConstants
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @NotNull
    @Valid
    private Profile profile;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant updatedAt;
}
