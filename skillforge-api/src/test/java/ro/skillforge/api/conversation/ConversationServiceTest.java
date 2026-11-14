package ro.skillforge.api.conversation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import ro.skillforge.api.MongoPersistenceTest;
import ro.skillforge.api.conversation.dto.ConversationResponse;
import ro.skillforge.api.conversation.dto.ModelUsageResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ro.skillforge.api.conversation.ConversationFixtures.*;

/**
 * Test de persistență ({@link MongoPersistenceTest}): serviciul de conversații pe un MongoDB real.
 */
@MongoPersistenceTest
@Import({ConversationService.class, ConversationMapper.class})
class ConversationServiceTest {
    private static final String ANA = "github:1";
    private static final String ION = "github:2";

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    @BeforeEach
    void startEmpty() {
        conversationRepository.deleteAll();
    }

    @Test
    void firstSaveCreatesTheConversation() {
        ConversationResponse saved = conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));

        assertEquals("c1", saved.id());
        assertNotNull(saved.version());
        assertEquals(1, saved.messages().size());
        assertEquals(1, conversationRepository.count());
    }

    @Test
    void saveWithTheCurrentVersionReplacesTheMessages() {
        ConversationResponse first = conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));

        ConversationResponse second = conversationService.save(ANA, "c1", snapshot(first.version(),
                question("m1", "Salut"),
                paidAnswer("m2", "anthropic", "claude-haiku", 100, 50)));

        assertTrue(second.version() > first.version());
        assertEquals(2, second.messages().size());
        assertEquals(first.createdAt(), second.createdAt());
        assertEquals(1, conversationRepository.count());
    }

    @Test
    void saveWithAStaleVersionIsRefused() {
        ConversationResponse first = conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));
        // Prima fereastră salvează: versiunea din baza de date crește.
        conversationService.save(ANA, "c1", snapshot(first.version(), question("m1", "Salut"), question("m2", "Din fereastra A")));

        // A doua fereastră are încă versiunea veche.
        assertThrows(OptimisticLockingFailureException.class, () ->
                conversationService.save(ANA, "c1", snapshot(first.version(), question("m1", "Salut"), question("m2", "Din fereastra B"))));

        assertEquals("Din fereastra A", conversationService.findById(ANA, "c1").messages().get(1).parts().getFirst().text());
    }

    @Test
    void saveAsNewOverAnExistingConversationIsRefused() {
        conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));

        assertThrows(DuplicateKeyException.class, () ->
                conversationService.save(ANA, "c1", snapshot(null, question("m1", "Altceva"))));
    }

    @Test
    void anotherUsersConversationIsNotFound() {
        ConversationResponse anas = conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));

        assertThrows(ConversationNotFoundException.class, () -> conversationService.findById(ION, "c1"));
        assertThrows(ConversationNotFoundException.class, () -> conversationService.delete(ION, "c1"));
        assertThrows(ConversationNotFoundException.class, () ->
                conversationService.save(ION, "c1", snapshot(anas.version(), question("m1", "Al meu acum"))));
        assertEquals(1, conversationService.findAllFor(ANA).size());
    }

    @Test
    void findAllReturnsOnlyMyConversationsMostRecentFirst() {
        conversationRepository.saveAll(List.of(
                stored("old", ANA, Instant.parse("2026-11-01T10:00:00Z")),
                stored("new", ANA, Instant.parse("2026-11-10T10:00:00Z")),
                stored("ions", ION, Instant.parse("2026-11-12T10:00:00Z"))));

        List<ConversationResponse> conversations = conversationService.findAllFor(ANA);

        assertEquals(List.of("new", "old"), conversations.stream().map(ConversationResponse::id).toList());
    }

    @Test
    void deletedConversationIsGone() {
        conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));

        conversationService.delete(ANA, "c1");

        assertEquals(0, conversationRepository.count());
        assertThrows(ConversationNotFoundException.class, () -> conversationService.findById(ANA, "c1"));
    }

    @Test
    void usageSumsPaidTokensPerModelAndIgnoresCachedAnswers() {
        conversationService.save(ANA, "c1", snapshot(null,
                question("m1", "Salut"),
                paidAnswer("m2", "anthropic", "claude-haiku", 100, 50),
                question("m3", "Și apoi?"),
                paidAnswer("m4", "anthropic", "claude-haiku", 200, 70),
                cachedAnswer("m5", "anthropic", "claude-haiku", 999, 999)));
        conversationService.save(ANA, "c2", snapshot(null,
                question("m1", "Altă întrebare"),
                paidAnswer("m2", "openai", "gpt-mini", 10, 20)));
        conversationService.save(ION, "c3", snapshot(null,
                question("m1", "Salut"),
                paidAnswer("m2", "anthropic", "claude-haiku", 5000, 5000)));

        List<ModelUsageResponse> usage = conversationService.usagePerModel(ANA);

        assertEquals(2, usage.size());
        ModelUsageResponse anthropic = usage.stream()
                .filter(row -> "anthropic".equals(row.providerId()))
                .findFirst()
                .orElseThrow();
        assertEquals("claude-haiku", anthropic.modelId());
        assertEquals(2, anthropic.responses());
        assertEquals(300, anthropic.inputTokens());
        assertEquals(120, anthropic.outputTokens());
        assertEquals(1.0, anthropic.inputPerMillionUsd());
        assertEquals(5.0, anthropic.outputPerMillionUsd());
    }

    @Test
    void usageOfAUserWithoutPaidAnswersIsEmpty() {
        conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut")));

        assertTrue(conversationService.usagePerModel(ANA).isEmpty());
    }

    private static Conversation stored(String id, String userId, Instant updatedAt) {
        return Conversation.builder()
                .id(id)
                .userId(userId)
                .title("O conversație")
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .messages(List.of(question("m1", "Salut")))
                .build();
    }
}
