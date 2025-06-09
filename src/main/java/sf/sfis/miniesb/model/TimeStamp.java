package sf.sfis.miniesb.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeStamp {
	private long timestamp;
    private String formattedTime;

    public TimeStamp() {
        this.timestamp = System.currentTimeMillis();
        this.formattedTime = formatTimestamp(this.timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.formattedTime = formatTimestamp(timestamp);
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    private String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime bangkokTime = instant.atZone(ZoneId.of("Asia/Bangkok")); // ✅ เวลาประเทศไทย
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        return formatter.format(bangkokTime);
    }
}
