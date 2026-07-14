package sf.sfis.miniesb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SubscribeScheduledService {
	@Autowired
	private SubscribeRequestService subscribeRequestService;

	LocalDateTime today;
	LocalDateTime tomorrowFrom;
	LocalDateTime tomorrowTo;

	/**
	 * เรียก subscribe ทันทีที่แอป start เสร็จ (re-subscribe หลัง restart).
	 * ใช้ ApplicationReadyEvent เพื่อให้ context/JMS connection พร้อมก่อน (ทำงานหลัง bean ทั้งหมดถูกสร้าง).
	 * ครอบ try/catch ไว้เพื่อไม่ให้การ subscribe ที่ fail (เช่น Artemis ยังไม่พร้อม) ทำให้ startup ล้ม.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void subscribeOnStartup() {
		try {
			log.info("miniESB started — running initial subscribe...");
			subscribeAfttab();
			// ถ้าต้องการ subscribe common counter (pl_desk) ตอน start ด้วย ให้เปิดบรรทัดนี้:
			// requestCcatab();
		} catch (Exception e) {
			log.error("subscribeOnStartup: ", e);
		}
	}
	
	@Scheduled(cron = "0 1 0 * * ?") // ทุกๆ 00.01 รับข้อมูล Update ระหว่างวัน และรองรับดึงข้อมูลของวันถัดไปด้วย
	@Scheduled(cron = "0 7 17 * * ?") // ทุกๆ 17.07 รับข้อมูล Update ระหว่างวัน และรองรับดึงข้อมูลของวันถัดไปด้วย
	public void subscribeAfttab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
		LocalDate now = LocalDate.now();
		// startTime = ต้นวันของวันนี้ (00:00:00.000000000)
		today = now.atStartOfDay();
		String startTime = today.format(formatter);
		// endTime = สิ้นวันของอีก 3 วันข้างหน้า (+72 ชม.) => 23:59:59.999999999
		tomorrowTo = now.plusDays(3).atTime(23, 59, 59, 999_999_999);
		String endTime = tomorrowTo.format(formatter);
		String dataType = "pl_turn";

		subscribeRequestService.subscribe(startTime, endTime, dataType);
	}

	@Scheduled(cron = "1 0 0 * * ?") //ทุกๆ เที่ยงคืนเลยไป 1 วิของวันถัดไป เพื่อรับข้อมูลของ Common Counter
	public void requestCcatab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
		LocalDate now = LocalDate.now();
		tomorrowFrom = now.plusDays(1).atTime(0, 0, 0);
		tomorrowTo = now.plusDays(1).atTime(23, 59, 59, 999_999_999);
		
		String startTime = tomorrowFrom.format(formatter);
		String endTime = tomorrowTo.format(formatter);
		String dataType = "pl_desk";

		//subscribeRequestService.subscribe(startTime, endTime, dataType);
		subscribeRequestService.requestDataset(startTime, endTime, dataType);
	}
	
	@Scheduled(cron = "0 0 17 * * ?") // ทุกๆ 5 โมงเย็น รับข้อมูลของ Flight วันถัดไป โดยใช้ Subscribe เดิมที่รันไว้ตอน 00.01
	public void requestAfttab() { 
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
		LocalDate now = LocalDate.now();
		tomorrowFrom = now.plusDays(1).atTime(0, 0, 0);
		tomorrowTo = now.plusDays(1).atTime(23, 59, 59, 999_999_999);
		
		String startTime = tomorrowFrom.format(formatter);
		String endTime = tomorrowTo.format(formatter);
		String dataType = "pl_turn";

		subscribeRequestService.requestDataset(startTime, endTime, dataType);
	}
}
