package sdp.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import sdp.config.JsonSerdesConfig;
import sdp.config.KafkaProperties;
import sdp.model.CountryEventAverage;
import sdp.model.Event;
import sdp.util.TestDataGenerator;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 6, topics = {
        "my_events",
        "enriched_events",
        "country_event_averages",
        "dlq"
})
class EventStreamProcessorIntegrationTest {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private JsonSerdesConfig jsonSerdesConfig;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Producer<Object, Object> producer;
    private Consumer<String, Event> enrichedConsumer;
    private Consumer<Windowed<String>, CountryEventAverage> averagesConsumer;
    private Consumer<String, String> dlqConsumer;

    @BeforeEach
    void setUp() {
        // Configure producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producer = new DefaultKafkaProducerFactory<>(producerProps).createProducer();

        // Configure consumers
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Map<String, Object> enrichedConsumerProps = new HashMap<>(consumerProps);
        enrichedConsumer = new DefaultKafkaConsumerFactory<>(
                enrichedConsumerProps,
                new StringDeserializer(),
                jsonSerdesConfig.event().deserializer()
        ).createConsumer();
        enrichedConsumer.subscribe(Collections.singletonList(kafkaProperties.getOutput()));

        Map<String, Object> averagesConsumerProps = new HashMap<>(consumerProps);
        averagesConsumer = new DefaultKafkaConsumerFactory<>(
                averagesConsumerProps,
                WindowedSerdes.timeWindowedSerdeFrom(String.class).deserializer(),
                jsonSerdesConfig.countryEventAverage().deserializer()
        ).createConsumer();
        averagesConsumer.subscribe(Collections.singletonList(kafkaProperties.getAverages()));

        Map<String, Object> dlqConsumerProps = new HashMap<>(consumerProps);
        dlqConsumer = new DefaultKafkaConsumerFactory<>(
                dlqConsumerProps,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
        dlqConsumer.subscribe(Collections.singletonList(kafkaProperties.getDlq()));
    }

    @AfterEach
    void tearDown() {
        producer.close();
        enrichedConsumer.close();
        averagesConsumer.close();
        dlqConsumer.close();
    }

    @Test
    void shouldProcessValidEvent() throws Exception {
        // Given
        Event validEvent = TestDataGenerator.createValidEvent();
        String eventJson = objectMapper.writeValueAsString(validEvent);

        // When - send multiple events to trigger window aggregation
        producer.send(new ProducerRecord<>(kafkaProperties.getInput(), validEvent.getCountry(), eventJson)).get();

        // Then - verify enriched event
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, Event> records = KafkaTestUtils.getRecords(enrichedConsumer, Duration.ofSeconds(1));
            assertFalse(records.isEmpty(), "Should have received enriched event");

            Event enrichedEvent = records.iterator().next().value();
            assertNotNull(enrichedEvent);
            assertEquals(validEvent.getUserId(), enrichedEvent.getUserId());
            assertEquals(validEvent.getCountry(), enrichedEvent.getCountry());
        });
    }

    @Test
    void shouldRouteToDLQForInvalidCountry() throws Exception {
        // Given
        Event invalidEvent = TestDataGenerator.createInvalidEvent();
        invalidEvent.setUserId(1L); // Valid user_id
        invalidEvent.setCountry("ZZ");
        String eventJson = objectMapper.writeValueAsString(invalidEvent);

        // When
        producer.send(new ProducerRecord<>(kafkaProperties.getInput(), "UNKNOWN", eventJson)).get();

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(dlqConsumer, Duration.ofSeconds(1));
            assertFalse(records.isEmpty(), "Should have received DLQ record");

            String dlqJson = records.iterator().next().value();
            assertNotNull(dlqJson);
            assertTrue(dlqJson.contains("invalid country"), "DLQ message should contain error about invalid country");
        });
    }

    @Test
    void shouldRouteToDLQForInvalidUserId() throws Exception {
        // Given
        Event invalidEvent = TestDataGenerator.createInvalidEvent();
        invalidEvent.setCountry("US"); // Valid country
        invalidEvent.setUserId(-1L);
        String eventJson = objectMapper.writeValueAsString(invalidEvent);

        // When
        producer.send(new ProducerRecord<>(kafkaProperties.getInput(), invalidEvent.getCountry(), eventJson)).get();

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(dlqConsumer, Duration.ofSeconds(1));
            assertFalse(records.isEmpty(), "Should have received DLQ record");

            String dlqJson = records.iterator().next().value();
            assertNotNull(dlqJson);
            assertTrue(dlqJson.contains("negative user_id"), "DLQ message should contain error about invalid user_id");
        });
    }

    @Test
    void shouldHandleMultipleEventsFromSameCountry() throws Exception {
        // Given
        Event event = TestDataGenerator.createValidEvent();

        // When - send multiple events
        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<>(kafkaProperties.getInput(), event.getCountry(), objectMapper.writeValueAsString(event))).get();
            Thread.sleep(100); // Small delay between events
        }

        // Then - verify enriched events
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, Event> records = KafkaTestUtils.getRecords(enrichedConsumer, Duration.ofSeconds(3));
            assertTrue(records.count() >= 3, "Should have received at least 3 enriched events");
        });
    }
}
