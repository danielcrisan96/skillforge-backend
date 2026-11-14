package ro.skillforge.api.user;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ro.skillforge.api.MongoPersistenceTest;
import ro.skillforge.api.conversation.Conversation;
import ro.skillforge.api.conversation.ConversationMapper;
import ro.skillforge.api.conversation.ConversationRepository;
import ro.skillforge.api.conversation.ConversationService;
import ro.skillforge.api.conversation.Message;
import ro.skillforge.api.conversation.MessagePart;
import ro.skillforge.api.user.dto.ProfileRequest;
import ro.skillforge.api.user.dto.ProfileResponse;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de persistență ({@link MongoPersistenceTest}): profilul unui utilizator și ștergerea contului.
 */
@MongoPersistenceTest
@Import({UserService.class, UserMapper.class, ConversationService.class, ConversationMapper.class})
class UserServiceTest {
    private static final String ANA = "github:1";
    private static final String ION = "github:2";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @BeforeEach
    void startEmpty() {
        userRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    void userWithoutASavedProfileIsNotFound() {
        assertThrows(UserNotFoundException.class, () -> userService.findProfile(ANA));
    }

    @Test
    void firstSaveCreatesTheUser() {
        ProfileResponse saved = userService.saveProfile(ANA, profile("C#, SQL", "AI Engineer"));

        assertEquals("AI Engineer", saved.goal());
        assertEquals(1, userRepository.count());
        assertEquals(saved, userService.findProfile(ANA));
    }

    @Test
    void secondSaveReplacesTheProfileAndKeepsOneUser() {
        userService.saveProfile(ANA, profile("C#, SQL", "AI Engineer"));

        userService.saveProfile(ANA, profile("Java, Spring", "Backend Engineer"));

        assertEquals(1, userRepository.count());
        ProfileResponse profile = userService.findProfile(ANA);
        assertEquals("Java, Spring", profile.stack());
        assertEquals("Backend Engineer", profile.goal());
        assertEquals(List.of(new Skill("Java", "avansat")), profile.skills());
    }

    @Test
    void invalidSkillLevelIsNeverWritten() {
        ProfileRequest request = new ProfileRequest("Ana", "Java", List.of(new Skill("Java", "expert")), "Un obiectiv");

        // Validarea de la ușă (400) e a controllerului; aici vedem a doua plasă: entitatea invalidă nu se salvează.
        assertThrows(ConstraintViolationException.class, () -> userService.saveProfile(ANA, request));
        assertEquals(0, userRepository.count());
    }

    @Test
    void deletingTheUserDeletesTheirConversationsOnly() {
        userService.saveProfile(ANA, profile("C#, SQL", "AI Engineer"));
        userService.saveProfile(ION, profile("Python", "Data Engineer"));
        conversationRepository.saveAll(List.of(conversation("a1", ANA), conversation("a2", ANA), conversation("i1", ION)));

        userService.delete(ANA);

        assertThrows(UserNotFoundException.class, () -> userService.findProfile(ANA));
        assertEquals(1, userRepository.count());
        assertEquals(List.of("i1"), conversationRepository.findAll().stream().map(Conversation::getId).toList());
    }

    private static ProfileRequest profile(String stack, String goal) {
        return new ProfileRequest("Ana", stack, List.of(new Skill("Java", "avansat")), goal);
    }

    private static Conversation conversation(String id, String userId) {
        Instant at = Instant.parse("2026-11-14T10:00:00Z");
        return Conversation.builder()
                .id(id)
                .userId(userId)
                .title("O conversație")
                .createdAt(at)
                .updatedAt(at)
                .messages(List.of(new Message("m1", "user", List.of(new MessagePart("text", "Salut")), null)))
                .build();
    }
}
