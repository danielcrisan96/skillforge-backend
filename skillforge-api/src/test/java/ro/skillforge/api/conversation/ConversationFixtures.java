package ro.skillforge.api.conversation;

import ro.skillforge.api.conversation.dto.SaveConversationRequest;

import java.util.List;

// Date de test pentru conversații, într-un singur loc, ca testele să arate doar ce e diferit de la un caz la altul.
final class ConversationFixtures {

    private ConversationFixtures() {
    }

    static SaveConversationRequest snapshot(Long version, Message... messages) {
        return SaveConversationRequest.builder()
                .title("Ce-mi lipsește pentru AI Engineer")
                .version(version)
                .messages(List.of(messages))
                .build();
    }

    static Message question(String id, String text) {
        return new Message(id, "user", List.of(new MessagePart("text", text)), null);
    }

    static Message paidAnswer(String id, String providerId, String modelId, long inputTokens, long outputTokens) {
        return answer(id, providerId, modelId, inputTokens, outputTokens, "model");
    }

    static Message cachedAnswer(String id, String providerId, String modelId, long inputTokens, long outputTokens) {
        return answer(id, providerId, modelId, inputTokens, outputTokens, "cache");
    }

    private static Message answer(String id, String providerId, String modelId, long inputTokens, long outputTokens,
                                  String source) {
        MessageMetadata metadata = new MessageMetadata(
                providerId,
                modelId,
                new Pricing(1.0, 5.0, "2026-09-16"),
                new TokenUsage(inputTokens, outputTokens),
                source);
        return new Message(id, "assistant", List.of(new MessagePart("text", "Un răspuns.")), metadata);
    }
}
