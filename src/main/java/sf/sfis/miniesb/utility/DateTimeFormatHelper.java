package sf.sfis.miniesb.utility;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class DateTimeFormatHelper {
	ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
	public String getTimestamp() {
        String formattedUtc = nowUtc.format(formatter);
        return formattedUtc;
	}
	
	public String convertFormatUTC(String dateTimeStr) {
		ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr);
		String formattedUtc = zdt.format(formatter);
		return formattedUtc;
	}

	public String convertLocalToUTC(String localDateTimeStr) {
		// สมมติว่า local time เป็นเวลาประเทศไทย
		ZoneId localZone = ZoneId.of("Asia/Bangkok");
		// แปลงเป็น LocalDateTime
		LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
		// ผูกกับโซนเวลา
		ZonedDateTime localZoned = localDateTime.atZone(localZone);
		// แปลงเป็นเวลา UTC
		ZonedDateTime nowUtc = localZoned.withZoneSameInstant(ZoneOffset.UTC);
		String formattedUtc = nowUtc.format(formatter);
		return formattedUtc;
	}
}
