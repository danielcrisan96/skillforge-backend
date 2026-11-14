package ro.skillforge.api.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

// Value object, încorporat în documentul utilizatorului: e mic, are o limită și nu se citește niciodată fără el.
@Builder(toBuilder = true)
public record Profile(@Size(max = 100) String name,
                      @Size(max = 500) String stack,
                      @NotNull @Size(max = 50) List<@Valid Skill> skills,
                      @Size(max = 1000) String goal) {

    public Profile {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
