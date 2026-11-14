package ro.skillforge.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;

// Testele își aduc propria bază de date: un MongoDB real, în Docker, pe un port ales la întâmplare.
@TestConfiguration
@ImportTestcontainers
public class TestcontainersConfiguration {

    // Un CÂMP static, nu o metodă: o metodă statică e ignorată fără niciun avertisment.
    // MongoDB 8.0 refuză să pornească pe kernelul Linux 6.19 și mai nou (SERVER-121912), ca pe runnerele noi de la
    // GitHub; setarea de mai jos îl lasă să pornească acolo. Fără ea, build-ul pică doar în CI.
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0")
            .withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
}
