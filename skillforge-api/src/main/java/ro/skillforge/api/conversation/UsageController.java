package ro.skillforge.api.conversation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.skillforge.api.conversation.dto.ModelUsageResponse;

import java.util.List;

import static ro.skillforge.api.shared.web.ApiKeyFilter.USER_ID_HEADER;

@RestController
@RequestMapping("/usage")
@RequiredArgsConstructor
public class UsageController {
    private final ConversationService conversationService;

    @GetMapping
    @ApiResponse(responseCode = "200", description = "Tokenii consumați de utilizator, pe model și pe preț; gol dacă nu are răspunsuri plătite")
    List<ModelUsageResponse> usagePerModel(@RequestHeader(USER_ID_HEADER) String userId) {
        return conversationService.usagePerModel(userId);
    }
}
