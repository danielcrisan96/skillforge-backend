package ro.skillforge.api.conversation;

import ro.skillforge.api.shared.error.NotFoundException;

public class ConversationNotFoundException extends NotFoundException {

    public ConversationNotFoundException(String conversationId) {
        super("Nu există nicio conversație cu id-ul " + conversationId);
    }
}
