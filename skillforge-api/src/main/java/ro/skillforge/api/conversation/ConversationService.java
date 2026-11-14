package ro.skillforge.api.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ro.skillforge.api.conversation.dto.ConversationResponse;
import ro.skillforge.api.conversation.dto.ModelUsageResponse;
import ro.skillforge.api.conversation.dto.SaveConversationRequest;
import ro.skillforge.api.user.UserDeleted;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationMapper conversationMapper;
    private final ConversationRepository conversationRepository;

    public List<ConversationResponse> findAllFor(String userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(conversationMapper::toResponse)
                .toList();
    }

    // Se caută după AMBELE id-uri: altfel, cine ghicește id-ul unei conversații ar putea citi conversația altcuiva.
    public ConversationResponse findById(String userId, String conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .map(conversationMapper::toResponse)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    /**
     * Salvează instantaneul unei conversații: o creează la prima salvare, o înlocuiește la următoarele.
     *
     * Două ferestre deschise pe aceeași conversație ar putea salva una peste cealaltă (lost update). De aceea
     * entitatea pleacă spre baza de date cu versiunea trimisă de CLIENT, nu cu cea citită acum: Spring Data scrie
     * „doar dacă versiunea e încă aceasta”, iar fereastra rămasă în urmă primește
     * {@code OptimisticLockingFailureException}, adică 409.
     *
     * Aici nu există {@code @Retryable}, spre deosebire de rezervarea unui loc: o reîncercare ar citi versiunea
     * nouă și ar scrie peste mesajele celeilalte ferestre, adică exact pierderea pe care o evităm. Clientul decide.
     */
    public ConversationResponse save(String userId, String conversationId, SaveConversationRequest request) {
        Optional<Conversation> existing = conversationRepository.findById(conversationId);

        // Id-ul există, dar e al altcuiva: 404, nu 403. Nu confirmăm nici măcar că id-ul e folosit.
        if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
            throw new ConversationNotFoundException(conversationId);
        }

        // MongoDB păstrează timpul cu precizie de milisecunde; trunchiem, ca obiectul citit să fie egal cu cel salvat.
        Instant now = Instant.now().truncatedTo(MILLIS);
        Instant createdAt = existing.map(Conversation::getCreatedAt)
                .or(() -> Optional.ofNullable(request.createdAt()).map(sent -> sent.truncatedTo(MILLIS)))
                .orElse(now);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .version(request.version())
                .userId(userId)
                .title(request.title())
                .createdAt(createdAt)
                .updatedAt(now)
                .messages(request.messages())
                .build();

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }

    public void delete(String userId, String conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversationRepository.delete(conversation);
    }

    public List<ModelUsageResponse> usagePerModel(String userId) {
        return conversationRepository.usagePerModel(userId);
    }

    // Utilizatorul și-a șters contul: conversațiile lui nu mai au ce căuta aici.
    @EventListener
    public void onUserDeleted(UserDeleted deleted) {
        conversationRepository.deleteByUserId(deleted.userId());
    }
}
