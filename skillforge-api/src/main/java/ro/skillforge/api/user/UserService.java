package ro.skillforge.api.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ro.skillforge.api.user.dto.ProfileRequest;
import ro.skillforge.api.user.dto.ProfileResponse;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.MILLIS;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProfileResponse findProfile(String userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // Nu există un „creează utilizator” separat: prima salvare a profilului creează documentul, următoarele îl
    // înlocuiesc. De aceea endpoint-ul e un PUT: trimis de două ori, lasă serverul ca după o singură trimitere.
    public ProfileResponse saveProfile(String userId, ProfileRequest request) {
        // MongoDB păstrează timpul cu precizie de milisecunde; trunchiem, ca obiectul citit să fie egal cu cel salvat.
        Instant now = Instant.now().truncatedTo(MILLIS);
        Profile profile = userMapper.toProfile(request);

        User user = userRepository.findById(userId)
                .map(existing -> existing.toBuilder()
                        .profile(profile)
                        .updatedAt(now)
                        .build())
                .orElseGet(() -> User.builder()
                        .id(userId)
                        .profile(profile)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        return userMapper.toResponse(userRepository.save(user));
    }

    // Șterge tot ce știm despre utilizator. Idempotent: un utilizator care nu există e deja șters.
    public void delete(String userId) {
        userRepository.deleteById(userId);
        applicationEventPublisher.publishEvent(new UserDeleted(userId));
    }
}
