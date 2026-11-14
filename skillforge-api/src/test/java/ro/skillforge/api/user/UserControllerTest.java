package ro.skillforge.api.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.skillforge.api.user.dto.ProfileResponse;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.skillforge.api.shared.web.ApiKeyFilter.API_KEY_HEADER;
import static ro.skillforge.api.shared.web.ApiKeyFilter.USER_ID_HEADER;

/**
 * Test de controller ({@code @WebMvcTest}, serviciul e un mock): partea de HTTP a profilului.
 */
@WebMvcTest(controllers = UserController.class, properties = "skillforge.api-key=test-key")
class UserControllerTest {
    private static final String ANA = "github:1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void profileIsServedForTheUserInTheHeader() throws Exception {
        when(userService.findProfile(ANA)).thenReturn(new ProfileResponse("Ana", "C#, SQL",
                List.of(new Skill("React", "începător")), "AI Engineer", Instant.parse("2026-11-14T10:00:00Z")));

        mockMvc.perform(get("/users/me/profile").header(API_KEY_HEADER, "test-key").header(USER_ID_HEADER, ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("AI Engineer"))
                .andExpect(jsonPath("$.skills[0].level").value("începător"));
    }

    @Test
    void userWithoutAProfileIsNotFound() throws Exception {
        when(userService.findProfile(ANA)).thenThrow(new UserNotFoundException(ANA));

        mockMvc.perform(get("/users/me/profile").header(API_KEY_HEADER, "test-key").header(USER_ID_HEADER, ANA))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    void validProfileIsSaved() throws Exception {
        when(userService.saveProfile(eq(ANA), any())).thenReturn(new ProfileResponse("Ana", "Java",
                List.of(), "Backend Engineer", Instant.parse("2026-11-14T10:00:00Z")));

        mockMvc.perform(put("/users/me/profile")
                        .header(API_KEY_HEADER, "test-key")
                        .header(USER_ID_HEADER, ANA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ana",
                                  "stack": "Java",
                                  "skills": [ { "name": "Java", "level": "avansat" } ],
                                  "goal": "Backend Engineer"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("Backend Engineer"));
    }

    @Test
    void unknownSkillLevelIsBadRequest() throws Exception {
        mockMvc.perform(put("/users/me/profile")
                        .header(API_KEY_HEADER, "test-key")
                        .header(USER_ID_HEADER, ANA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "skills": [ { "name": "Java", "level": "expert" } ] }"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(userService, never()).saveProfile(any(), any());
    }

    @Test
    void withoutTheApiKeyNothingIsDeleted() throws Exception {
        mockMvc.perform(delete("/users/me").header(USER_ID_HEADER, ANA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteAnswersNoContent() throws Exception {
        mockMvc.perform(delete("/users/me").header(API_KEY_HEADER, "test-key").header(USER_ID_HEADER, ANA))
                .andExpect(status().isNoContent());

        verify(userService).delete(ANA);
    }
}
