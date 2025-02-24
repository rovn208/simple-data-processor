package sdp.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static String formatDate(long epochMilli) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);
        return formatter.format(Instant.ofEpochMilli(epochMilli));
    }
}
