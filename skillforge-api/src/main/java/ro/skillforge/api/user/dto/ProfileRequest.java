package ro.skillforge.api.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.skillforge.api.user.Skill;

import java.util.List;

// Ce are voie să trimită clientul. Aceleași limite ca pe entitate, verificate aici ca răspunsul să fie un 400 cu
// numele câmpului greșit, nu o eroare la salvare.
public record ProfileRequest(@Size(max = 100) String name,
                             @Size(max = 500) String stack,
                             @NotNull @Size(max = 50) List<@Valid Skill> skills,
                             @Size(max = 1000) String goal) {
}
