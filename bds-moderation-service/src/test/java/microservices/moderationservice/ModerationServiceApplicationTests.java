package microservices.moderationservice;

import microservices.moderationservice.moderation.repository.mongo.ViolationReportDetailsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ModerationServiceApplicationTests {

    @MockBean
    private ViolationReportDetailsRepository violationReportDetailsRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
