package sdp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("page_url")
    private String pageUrl;

    @JsonProperty("button_id")
    private String buttonId;

    @JsonProperty("device_type")
    private String deviceType;

    private String country;

    private String timestamp;
}
