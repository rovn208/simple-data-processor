package sdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;
import sdp.model.CountryEventAverage;
import sdp.model.DeadLetterQueueRecord;
import sdp.model.Event;

@Component
public class JsonSerdesConfig {

    @Bean
    public JsonSerde<Event> event() {
        return new JsonSerde<>(Event.class);
    }

    @Bean
    public JsonSerde<CountryEventAverage> countryEventAverage() {
        return new JsonSerde<>(CountryEventAverage.class);
    }

    @Bean
    public JsonSerde<DeadLetterQueueRecord> dlq() {
        return new JsonSerde<>(DeadLetterQueueRecord.class);
    }
}
