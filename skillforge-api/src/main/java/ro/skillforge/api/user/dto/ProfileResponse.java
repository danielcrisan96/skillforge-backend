package ro.skillforge.api.user.dto;

import ro.skillforge.api.user.Skill;

import java.time.Instant;
import java.util.List;

public record ProfileResponse(String name,
                              String stack,
                              List<Skill> skills,
                              String goal,
                              Instant updatedAt) {
}
