package sdp.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sdp.config.JsonSerdesConfig;
import sdp.config.KafkaProperties;
import sdp.config.SDPProperties;
import sdp.model.CountryEventAverage;
import sdp.model.DeadLetterQueueRecord;
import sdp.model.Event;
import sdp.util.DateUtil;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStreamProcessor {
    private final KafkaProperties kafkaProperties;
    private final SDPProperties sdpProperties;
    private final JsonSerdesConfig jsonSerdesConfig;
    private final int AGGREGATION_DURATION_IN_MINUTES = 5;

    @Autowired
    void buildPipeline(StreamsBuilder streamsBuilder) {
        KStream<String, String> sourceStream = streamsBuilder.stream(
                kafkaProperties.getInput(),
                Consumed.with(Serdes.String(), Serdes.String())
        );

        sourceStream
                .mapValues(this::parseEvent)
                .selectKey((key, event) -> key == null && event != null ? event.getCountry() : "UNKNOWN")
                .split()
                .branch(isValidEvent(), Branched.withConsumer(this::processValidEvents))
                .defaultBranch(Branched.withConsumer(this::processInvalidEvents));
    }

    private Predicate<String, Event> isValidEvent() {
        return (key, event) -> event.getUserId() > 0 && isValidCountry(event.getCountry());
    }

    private boolean isValidCountry(String country) {
        return sdpProperties.getSupportedCountries().contains(country);
    }

    private Event parseEvent(String value) {
        try {
            return jsonSerdesConfig.event().deserializer().deserialize(null, value.getBytes());
        } catch (Exception e) {
            log.error("Error parsing event: {}", value, e);
            return null;
        }
    }


    private void processValidEvents(KStream<String, Event> validEvents) {
        // Send to enriched events topic
        validEvents.to(
                kafkaProperties.getOutput(),
                Produced.with(Serdes.String(), jsonSerdesConfig.event())
        );

        // Calculate rolling averages by country
        validEvents
                .groupByKey()
                .windowedBy(TimeWindows.of(Duration.ofMinutes(AGGREGATION_DURATION_IN_MINUTES)))
                .count()
                .toStream()
                .mapValues((window, count) -> CountryEventAverage.builder()
                        .country(window.key())
                        .windowStart(DateUtil.formatDate(window.window().startTime().toEpochMilli()))
                        .windowEnd(DateUtil.formatDate(window.window().endTime().toEpochMilli()))
                        .averageEvents((double) count / AGGREGATION_DURATION_IN_MINUTES)
                        .build()
                )
                .peek((key, value) -> System.out.println("Value " + value))
                .to(
                        kafkaProperties.getAverages(),
                        Produced.with(
                                WindowedSerdes.timeWindowedSerdeFrom(String.class),
                                jsonSerdesConfig.countryEventAverage()
                        )
                );
    }

    private void processInvalidEvents(KStream<String, Event> invalidEvents) {
        invalidEvents
                .mapValues(event -> DeadLetterQueueRecord.builder()
                        .errorMessage("Invalid event: " +
                                (event.getUserId() < 0 ? "negative user_id" : "invalid country"))
                        .rawData(event.toString())
                        .timestamp(DateUtil.formatDate(Instant.now().toEpochMilli()))
                        .build())
                .to(
                        kafkaProperties.getDlq(),
                        Produced.with(Serdes.String(), jsonSerdesConfig.dlq())
                );
    }
} 
