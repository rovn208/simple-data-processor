package sdp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeadLetterQueueRecord {

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("raw_data")
    private String rawData;

    private Long timestamp;
} 
