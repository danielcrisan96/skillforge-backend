package ro.skillforge.api.conversation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import ro.skillforge.api.MongoPersistenceTest;
import ro.skillforge.api.conversation.dto.ConversationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.skillforge.api.conversation.ConversationFixtures.question;
import static ro.skillforge.api.conversation.ConversationFixtures.snapshot;

/**
 * Test de persistență ({@link MongoPersistenceTest}): 10 ferestre care salvează aceeași conversație în același moment,
 * toate cu aceeași versiune. Una singură câștigă; celelalte sunt refuzate, nu scrise una peste alta. Scoate
 * {@code @Version} din {@link Conversation} și testul pică: asta e dovada că testează ceva.
 */
@MongoPersistenceTest
@Import({ConversationService.class, ConversationMapper.class})
class ConcurrentSaveTest {
    private static final String ANA = "github:1";
    private static final int WINDOWS = 10;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    private Long versionEveryWindowHas;

    @BeforeEach
    void startWithOneConversation() {
        conversationRepository.deleteAll();
        versionEveryWindowHas = conversationService.save(ANA, "c1", snapshot(null, question("m1", "Salut"))).version();
    }

    @Test
    void onlyOneOfTheConcurrentSavesWins() throws Exception {
        // Fiecare thread așteaptă la linia de start, ca toate cele 10 salvări să plece în același moment.
        CountDownLatch ready = new CountDownLatch(WINDOWS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ConversationResponse>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(WINDOWS)) {
            for (int i = 0; i < WINDOWS; i++) {
                String text = "Din fereastra " + i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return conversationService.save(ANA, "c1",
                            snapshot(versionEveryWindowHas, question("m1", "Salut"), question("m2", text)));
                }));
            }
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS), "toate threadurile la linia de start");
            } finally {
                // Linia de start se deschide mereu, ca o așteptare eșuată să încheie testul, nu să blocheze pool.close().
                start.countDown();
            }
        }

        int saved = 0;
        int refused = 0;
        for (Future<ConversationResponse> future : futures) {
            try {
                future.get();
                saved++;
            } catch (ExecutionException e) {
                assertInstanceOf(OptimisticLockingFailureException.class, e.getCause());
                refused++;
            }
        }

        assertEquals(1, saved);
        assertEquals(WINDOWS - 1, refused);
        assertEquals(2, conversationService.findById(ANA, "c1").messages().size());
    }
}
