package ro.skillforge.api.conversation;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

// Raportul de consum e un fragment: UsageReportRepository îl declară, UsageReportRepositoryImpl îl construiește cu
// MongoTemplate. Serviciul vede un singur repository.
public interface ConversationRepository extends MongoRepository<Conversation, String>, UsageReportRepository {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<Conversation> findByIdAndUserId(String id, String userId);

    void deleteByUserId(String userId);
}
