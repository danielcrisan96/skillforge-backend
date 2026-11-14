package ro.skillforge.api.conversation;

import org.springframework.stereotype.Component;
import ro.skillforge.api.conversation.dto.ConversationResponse;

/**
 * Traduce entitatea {@link Conversation} în răspunsul trimis clientului.
 */
@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getVersion(),
                conversation.getMessages());
    }
}
