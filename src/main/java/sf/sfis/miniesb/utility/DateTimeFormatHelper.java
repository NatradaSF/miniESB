package sf.sfis.miniesb.utility;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class DateTimeFormatHelper {
	ZoneId utcZone = ZoneOffset.UTC;
	ZoneId localZone = ZoneId.of("Asia/Bangkok");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
//	public String getTimestamp() {
//		ZonedDateTime nowUtc = ZonedDateTime.now(utcZone);
//        String formattedUtc = nowUtc.format(formatter);
//        return formattedUtc;
//	}
//	
//	public String convertFormatUTC(String dateTimeStr) {
//		ZonedDateTime zdt = ZonedDateTime.parse(dateTimeStr);
//		String formattedUtc = zdt.format(formatter);
//		return formattedUtc;
//	}

	public String convertUTCToLocal(String utcDateTimeStr) {
		// แปลงเป็น LocalDateTime
		LocalDateTime utcDateTime = LocalDateTime.parse(utcDateTimeStr, formatter);
		// ผูกกับโซนเวลา
		ZonedDateTime utcZoned = utcDateTime.atZone(utcZone);
		// แปลงเป็นเวลา UTC
		ZonedDateTime zonedLocal = utcZoned.withZoneSameInstant(localZone);
		String formattedLocal = formatter.format(zonedLocal);
		return formattedLocal;
	}

	public String convertLocalToUTC(String localDateTimeStr) {
		// แปลงเป็น LocalDateTime
		LocalDateTime localDateTime = LocalDateTime.parse(localDateTimeStr);
		// ผูกกับโซนเวลา
		ZonedDateTime localZoned = localDateTime.atZone(localZone);
		// แปลงเป็นเวลา UTC
		ZonedDateTime zonedUtc = localZoned.withZoneSameInstant(utcZone);
		String formattedUtc = zonedUtc.format(formatter);
		return formattedUtc;
	}
}
