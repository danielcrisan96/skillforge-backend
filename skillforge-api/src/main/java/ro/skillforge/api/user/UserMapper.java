package ro.skillforge.api.user;

import org.springframework.stereotype.Component;
import ro.skillforge.api.user.dto.ProfileRequest;
import ro.skillforge.api.user.dto.ProfileResponse;

/**
 * Traduce între entitatea {@link User} și DTO-urile de profil, astfel încât niciuna dintre părți nu știe forma
 * celeilalte.
 */
@Component
public class UserMapper {

    public Profile toProfile(ProfileRequest request) {
        return Profile.builder()
                .name(request.name())
                .stack(request.stack())
                .skills(request.skills())
                .goal(request.goal())
                .build();
    }

    public ProfileResponse toResponse(User user) {
        Profile profile = user.getProfile();
        return new ProfileResponse(
                profile.name(),
                profile.stack(),
                profile.skills(),
                profile.goal(),
                user.getUpdatedAt());
    }
}
