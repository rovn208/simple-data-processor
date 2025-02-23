package sdp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "sdp")
public class SDPProperties {
    private List<String> supportedCountries = List.of("US", "CA", "UK", "DE", "FR", "JP");
}
