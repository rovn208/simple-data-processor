package sdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaProperties {
    private String input;
    private String output;
    private String averages;
    private String dlq;
} 