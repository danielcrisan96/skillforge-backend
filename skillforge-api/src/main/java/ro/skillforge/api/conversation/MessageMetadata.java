package ro.skillforge.api.conversation;

import jakarta.validation.constraints.Size;

// Ce lipește aplicația web pe un răspuns al asistentului: cine a răspuns, la ce preț, cu ce consum și de unde a
// venit răspunsul („model”, plătit, sau „cache”, fără niciun apel).
public record MessageMetadata(@Size(max = 40) String providerId,
                              @Size(max = 100) String modelId,
                              Pricing pricing,
                              TokenUsage usage,
                              @Size(max = 20) String source) {
}
