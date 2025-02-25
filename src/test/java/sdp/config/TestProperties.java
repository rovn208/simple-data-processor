package sdp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestProperties {

    @Bean
    @Primary
    public KafkaProperties kafkaProperties() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setInput("test-input");
        kafkaProperties.setOutput("test-output");
        kafkaProperties.setAverages("test-averages");
        kafkaProperties.setDlq("test-dlq");
        return kafkaProperties;
    }
}
