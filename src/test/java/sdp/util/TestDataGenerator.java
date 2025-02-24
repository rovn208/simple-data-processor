package sdp.util;

import sdp.model.Event;

import java.time.Instant;

public class TestDataGenerator {
    public static Event createValidEvent() {
        Event event = new Event();
        event.setEventType("page_view");
        event.setUserId(123L);
        event.setPageUrl("/test");
        event.setButtonId("btn-1");
        event.setTimestamp(Instant.now().getEpochSecond());
        event.setCountry("US");
        event.setDeviceType("mobile");
        return event;
    }

    public static Event createInvalidEvent() {
        Event event = new Event();
        event.setEventType("page_view");
        event.setUserId(-1L); // Invalid user ID
        event.setPageUrl("/test");
        event.setButtonId("btn-1");
        event.setTimestamp(Instant.now().getEpochSecond());
        event.setCountry("ZZ"); // Invalid country
        event.setDeviceType("mobile");
        return event;
    }
}
