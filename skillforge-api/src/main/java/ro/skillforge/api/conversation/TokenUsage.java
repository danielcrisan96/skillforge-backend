package ro.skillforge.api.conversation;

// Consumul raportat de provider. Long, nu long: o valoare lipsă înseamnă „necunoscut” și trebuie să rămână null,
// nu să devină zero (care ar arăta ca un răspuns gratuit).
public record TokenUsage(Long inputTokens,
                         Long outputTokens) {
}
