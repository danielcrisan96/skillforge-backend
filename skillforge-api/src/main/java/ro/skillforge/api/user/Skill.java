package ro.skillforge.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Value object: două skill-uri cu același nume și nivel sunt același skill. Nivelurile sunt exact cele din
// aplicația web (SKILL_LEVELS din src/lib/types.ts).
public record Skill(@NotBlank @Size(max = 60) String name,
                    @NotBlank @Pattern(regexp = "începător|intermediar|avansat") String level) {
}
