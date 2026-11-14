package ro.skillforge.api.user;

import ro.skillforge.api.shared.error.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String userId) {
        super("Utilizatorul " + userId + " nu are încă un profil salvat");
    }
}
