package ro.skillforge.api.conversation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.skillforge.api.conversation.dto.ConversationResponse;

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
import static ro.skillforge.api.conversation.ConversationFixtures.question;
import static ro.skillforge.api.shared.web.ApiKeyFilter.API_KEY_HEADER;
import static ro.skillforge.api.shared.web.ApiKeyFilter.USER_ID_HEADER;

/**
 * Test de controller ({@code @WebMvcTest}, serviciul e un mock): partea de HTTP a conversațiilor, cu filtrul de
 * cheie în față și cu codurile pe care {@code GlobalExceptionHandler} le dă excepțiilor.
 */
@WebMvcTest(controllers = ConversationController.class, properties = "skillforge.api-key=test-key")
class ConversationControllerTest {
    private static final String ANA = "github:1";
    private static final String VALID_SNAPSHOT = """
            {
              "title": "Ce-mi lipsește pentru AI Engineer",
              "version": 3,
              "messages": [
                { "id": "m1", "role": "user", "parts": [ { "type": "text", "text": "Salut" } ] }
              ]
            }""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @Test
    void withoutTheApiKeyNothingIsServed() throws Exception {
        mockMvc.perform(get("/conversations").header(USER_ID_HEADER, ANA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(conversationService);
    }

    @Test
    void withAWrongApiKeyNothingIsServed() throws Exception {
        mockMvc.perform(get("/conversations").header(API_KEY_HEADER, "ghicit").header(USER_ID_HEADER, ANA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(conversationService);
    }

    @Test
    void withoutAUserIdTheRequestIsBad() throws Exception {
        mockMvc.perform(get("/conversations").header(API_KEY_HEADER, "test-key"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(conversationService);
    }

    @Test
    void listIsServedForTheUserInTheHeader() throws Exception {
        when(conversationService.findAllFor(ANA)).thenReturn(List.of(response("c1", 0L)));

        mockMvc.perform(get("/conversations").header(API_KEY_HEADER, "test-key").header(USER_ID_HEADER, ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("c1"))
                .andExpect(jsonPath("$[0].messages[0].parts[0].text").value("Salut"))
                // Un câmp fără valoare lipsește din JSON, nu apare ca null.
                .andExpect(jsonPath("$[0].messages[0].metadata").doesNotExist());
    }

    @Test
    void validSnapshotIsSavedAndAnswersWithTheNewVersion() throws Exception {
        when(conversationService.save(eq(ANA), eq("c1"), any())).thenReturn(response("c1", 4L));

        mockMvc.perform(put("/conversations/c1")
                        .header(API_KEY_HEADER, "test-key")
                        .header(USER_ID_HEADER, ANA)
                        .contentType(APPLICATION_JSON)
                        .content(VALID_SNAPSHOT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void snapshotWithoutATitleIsBadRequest() throws Exception {
        mockMvc.perform(put("/conversations/c1")
                        .header(API_KEY_HEADER, "test-key")
                        .header(USER_ID_HEADER, ANA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "title": " ", "messages": [] }"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(conversationService, never()).save(any(), any(), any());
    }

    @Test
    void messageWithAnUnknownRoleIsBadRequest() throws Exception {
        mockMvc.perform(put("/conversations/c1")
                        .header(API_KEY_HEADER, "test-key")
                        .header(USER_ID_HEADER, ANA)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Titlu",
                                  "messages": [ { "id": "m1", "role": "hacker", "parts": [] } ]
                                }"""))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).save(any(), any(), any());
    }

    @Test
    void staleVersionIsConflict() throws Exception {
        when(conversationService.save(eq(ANA), eq("c1"), any()))
                .thenThrow(new OptimisticLockingFailureException("versiune veche"));

        mockMvc.perform(put("/conversations/c1")
                        .header(API_KEY_HEADER, "test-key")
                        .header(USER_ID_HEADER, ANA)
                        .contentType(APPLICATION_JSON)
                        .content(VALID_SNAPSHOT))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unknownConversationIsNotFound() throws Exception {
        when(conversationService.findById(ANA, "nu-exista")).thenThrow(new ConversationNotFoundException("nu-exista"));

        mockMvc.perform(get("/conversations/nu-exista").header(API_KEY_HEADER, "test-key").header(USER_ID_HEADER, ANA))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    void deleteAnswersNoContent() throws Exception {
        mockMvc.perform(delete("/conversations/c1").header(API_KEY_HEADER, "test-key").header(USER_ID_HEADER, ANA))
                .andExpect(status().isNoContent());

        verify(conversationService).delete(ANA, "c1");
    }

    private static ConversationResponse response(String id, Long version) {
        Instant at = Instant.parse("2026-11-14T10:00:00Z");
        return new ConversationResponse(id, "Ce-mi lipsește pentru AI Engineer", at, at, version,
                List.of(question("m1", "Salut")));
    }
}
