package net.skycomposer.betting.common;

import lombok.SneakyThrows;
import net.skycomposer.betting.bettinghouse.BettingHouseTestDataService;
import net.skycomposer.betting.client.KafkaClient;
import net.skycomposer.betting.config.MockHelper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class E2eTest {

    @Value("${security.oauth2.username}")
    private String securityOauth2Username;

    @Value("${security.oauth2.password}")
    private String securityOauth2Password;

    @Autowired
    private MockHelper mockHelper;

    @Autowired
    private BettingHouseTestDataService customerTestDataService;

    @Autowired
    private KafkaClient kafkaClient;

    @BeforeEach
    @SneakyThrows
    void cleanup() {
        kafkaClient.clearMessages("market-projection");
        kafkaClient.clearMessages("bet-projection");
        mockHelper.mockCredentials(securityOauth2Username, securityOauth2Password);
        customerTestDataService.resetDatabase();
        //TimeUnit.MILLISECONDS.sleep(Duration.ofSeconds(1).toMillis());
    }
}
