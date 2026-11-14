package ro.skillforge.api.conversation;

import ro.skillforge.api.conversation.dto.ModelUsageResponse;

import java.util.List;

public interface UsageReportRepository {

    // Gol când utilizatorul nu are niciun răspuns plătit.
    List<ModelUsageResponse> usagePerModel(String userId);
}
