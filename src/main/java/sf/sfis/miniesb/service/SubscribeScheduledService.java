package sf.sfis.miniesb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SubscribeScheduledService {
	@Autowired
	private SubscribeRequestService subscribeRequestService;

	LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
	LocalDateTime tomorrowFrom = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
	LocalDateTime tomorrowTo = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

	@Scheduled(cron = "1 0 0 * * ?") //ทุกๆ เที่ยงคืนเลยไป 1 วิของวันถัดไป เพื่อรับข้อมูลของ Common Counter
	public void subscribeAndRequestCcatab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//		tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		String startTime = tomorrowFrom.format(formatter);
//		tomorrowTo = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
		String endTime = tomorrowTo.format(formatter);
		String dataType = "pl_desk";

		subscribeRequestService.subscribe(startTime, endTime, dataType);
		subscribeRequestService.requestDataset(startTime, endTime, dataType);
	}
	
	@Scheduled(cron = "0 1 0 * * ?") // ทุกๆ 00.01 รับข้อมูล Update ระหว่างวัน และรองรับดึงข้อมูลของวันถัดไปด้วย
	@Scheduled(cron = "0 7 17 * * ?") // ทุกๆ 17.07 รับข้อมูล Update ระหว่างวัน และรองรับดึงข้อมูลของวันถัดไปด้วย
	public void subscribeAfttab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//		today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
		String startTime = today.format(formatter);
//		tomorrow = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
		String endTime = tomorrowTo.format(formatter);
		String dataType = "pl_turn";

		subscribeRequestService.subscribe(startTime, endTime, dataType);
	}
	
	@Scheduled(cron = "0 0 17 * * ?") // ทุกๆ 5 โมงเย็น รับข้อมูลของ Flight วันถัดไป โดยใช้ Subscribe เดิมที่รันไว้ตอน 00.01
	public void requestAfttab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//		LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		String startTime = tomorrowFrom.format(formatter);
//		tomorrow = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
		String endTime = tomorrowTo.format(formatter);
		String dataType = "pl_turn";

		subscribeRequestService.requestDataset(startTime, endTime, dataType);
	}
}
