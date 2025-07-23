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

	@Scheduled(cron = "0 0 17 * * ?") // ทุกๆ 4 โมงเย็น
	public void subscribeAndRequestAfttab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		String startTime = tomorrow.format(formatter);
		tomorrow = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
		String endTime = tomorrow.format(formatter);
		String dataType = "pl_turn";

		subscribeRequestService.subscribe(startTime, endTime, dataType);
		subscribeRequestService.requestDataset(dataType);
	}
	
	@Scheduled(cron = "0 0 19 * * ?") // ทุกๆ 6 โมงเย็น
	public void subscribeAndRequestCcatab() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		String startTime = tomorrow.format(formatter);
		tomorrow = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
		String endTime = tomorrow.format(formatter);
		String dataType = "pl_desk";

		subscribeRequestService.subscribe(startTime, endTime, dataType);
		subscribeRequestService.requestDataset(dataType);
	}
}
