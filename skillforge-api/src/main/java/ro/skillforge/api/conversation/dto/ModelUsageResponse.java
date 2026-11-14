package ro.skillforge.api.conversation.dto;

import lombok.experimental.FieldNameConstants;

// Consumul pe un model, la un preț. Costul NU e aici, intenționat: întoarcem ingredientele (tokeni și preț), iar
// formula rămâne într-un singur loc, în aplicația web (src/lib/cost.ts). Două formule pentru același cost s-ar
// despărți la prima schimbare.
@FieldNameConstants
public record ModelUsageResponse(String providerId,
                                 String modelId,
                                 Double inputPerMillionUsd,
                                 Double outputPerMillionUsd,
                                 long responses,
                                 long inputTokens,
                                 long outputTokens) {
}
