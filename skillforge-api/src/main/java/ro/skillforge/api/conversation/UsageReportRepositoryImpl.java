package ro.skillforge.api.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import ro.skillforge.api.conversation.dto.ModelUsageResponse;

import java.util.List;

import static ro.skillforge.api.conversation.Conversation.Fields.messages;
import static ro.skillforge.api.conversation.Conversation.Fields.userId;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.inputPerMillionUsd;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.inputTokens;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.modelId;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.outputPerMillionUsd;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.outputTokens;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.providerId;
import static ro.skillforge.api.conversation.dto.ModelUsageResponse.Fields.responses;

@RequiredArgsConstructor
public class UsageReportRepositoryImpl implements UsageReportRepository {
    private static final String METADATA = messages + ".metadata.";

    private final MongoTemplate mongoTemplate;

    // Tokenii sunt adunați de MongoDB, nu în aplicație: toate conversațiile rămân în baza de date, se întorc doar
    // câteva rânduri.
    @Override
    public List<ModelUsageResponse> usagePerModel(String forUserId) {
        Aggregation pipeline = Aggregation.newAggregation(
                // $match primul: indexul pe userId ajută, iar restul pașilor văd doar conversațiile acestui utilizator.
                Aggregation.match(Criteria.where(userId).is(forUserId)),
                // $unwind: un document pentru fiecare mesaj, ca mesajele să poată fi filtrate și grupate.
                Aggregation.unwind(messages),
                // Doar răspunsurile plătite. Cele din cache n-au costat nimic, iar mesajele utilizatorului n-au metadate.
                Aggregation.match(Criteria.where(METADATA + "source").is("model")),
                // $project: câmpurile de care are nevoie gruparea, scoase din adâncime sub nume simple.
                Aggregation.project()
                        .and(METADATA + "providerId").as(providerId)
                        .and(METADATA + "modelId").as(modelId)
                        .and(METADATA + "pricing.inputPerMillionUsd").as(inputPerMillionUsd)
                        .and(METADATA + "pricing.outputPerMillionUsd").as(outputPerMillionUsd)
                        .and(METADATA + "usage.inputTokens").as(inputTokens)
                        .and(METADATA + "usage.outputTokens").as(outputTokens),
                // $group: un rând per model ȘI per preț. Prețul face parte din cheie, ca un model al cărui preț s-a
                // schimbat să apară pe două rânduri, fiecare cu tokenii plătiți la prețul lui.
                Aggregation.group(providerId, modelId, inputPerMillionUsd, outputPerMillionUsd)
                        .count().as(responses)
                        .sum(inputTokens).as(inputTokens)
                        .sum(outputTokens).as(outputTokens),
                // Cheia grupării ajunge în _id; ultimul pas o aduce înapoi sub numele câmpurilor din răspuns.
                Aggregation.project(providerId, modelId, inputPerMillionUsd, outputPerMillionUsd,
                        responses, inputTokens, outputTokens));

        return mongoTemplate.aggregate(pipeline, Conversation.class, ModelUsageResponse.class).getMappedResults();
    }
}
