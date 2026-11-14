package ro.skillforge.api.conversation.dto;

import ro.skillforge.api.conversation.Message;

import java.time.Instant;
import java.util.List;

// userId lipsește intenționat: clientul știe deja cine e, iar un id în plus în răspuns e un id în plus în loguri.
public record ConversationResponse(String id,
                                   String title,
                                   Instant createdAt,
                                   Instant updatedAt,
                                   Long version,
                                   List<Message> messages) {
}
