package sf.sfis.miniesb.controller;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.service.RedisService;

@RestController
@Component
@RequiredArgsConstructor
public class RedisController {
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisController.class);

	@Value("${jboss.ip}")
	private String serverIp;

	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final RedisService redisService; // Inject RedisService
	private final StringRedisTemplate redisTemplate;

	// ✅ API ใส่ข้อมูลใน Redis (แทนค่าตัวเก่าทุกครั้ง)
//    @PostMapping("/cache-data")
//    public ResponseEntity<String> cacheData(@RequestParam String hopo) {
	public String saveData(String hopo) {
		try {
//            String ds = ds.toUpperCase();
			List<String> lstDatasource = Arrays.asList("DEP", "ARR", "GATE", "BELT", "COUNTER");
			hopo = hopo.toUpperCase();

			String url = "";
			StringBuilder result = new StringBuilder();
			for (String ds : lstDatasource) {
				url = String.format("http://" + serverIp + "/FIDSDataServicesBKK/getDataByDSName?DS=%s&HOPO=%s", ds,
						hopo);
//				String response = restTemplate.getForObject(url, String.class);
				String response = webClient.get().uri(url).retrieve().bodyToMono(String.class).block();

				List<Map<String, Object>> dataList = objectMapper.readValue(response,
						new TypeReference<List<Map<String, Object>>>() {
						});

//				// ใช้ RedisService ลบข้อมูลเก่าก่อน
//				int deleted = redisService.deleteDataInternal(ds, hopo);

				// ใช้ RedisService บันทึกข้อมูลใหม่
				result.append(redisService.saveDataToRedis(dataList, ds, hopo));
				result.append(System.lineSeparator());

//				String timestamp = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
//						.withZone(ZoneId.of("Asia/Bangkok")).format(Instant.now());

//				String output = String.format("HOPO=%s DS=%s %s\nDeleted keys: %d\nData : %s", hopo, ds, timestamp,
//						deleted, result);
//				LOGGER.info(output);
			}
			LOGGER.info(result.toString());
			return result.toString();
		} catch (Exception e) {
			LOGGER.error("saveData: ", e);
//			e.printStackTrace();
			return "❌ Failed: " + e.getMessage();
		}
	}

	@GetMapping("/getDataByDSName")
	public ResponseEntity<?> getData(@RequestParam String DS, @RequestParam String HOPO) {
		try {
			DS = DS.toUpperCase();
			HOPO = HOPO.toUpperCase();

			String key = HOPO + "/" + DS;
//			LOGGER.info(key);
			String jsonData = redisTemplate.opsForValue().get(key);
//			LOGGER.info(jsonData);
			if (jsonData == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("❌ No cached data found for HOPO=" + HOPO + ", DS=" + DS);
			}

			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonData);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("❌ Failed to get cached data: " + e.getMessage());
		}
	}
//
	@GetMapping("/getTimestamp")
	public ResponseEntity<String> getTimestamp(@RequestParam String HOPO) {
		try {
			HOPO = HOPO.toUpperCase();

			String key = HOPO + "/timestamp";
			String timestamp = redisTemplate.opsForValue().get(key);

//			LOGGER.info(timestamp);
			if (timestamp == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("❌ No timestamp found for HOPO=" + HOPO);
			}

			SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = inputFormat.parse(timestamp);
            timestamp = outputFormat.format(date);
			
			return ResponseEntity.ok("Current Timestamp : "+timestamp);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("❌ Failed to get timestamp: " + e.getMessage());
		}
	}

//    @DeleteMapping("/delete-data")
//    public ResponseEntity<String> deleteData(
//            @RequestParam String ds,
//            @RequestParam String hopo) {
//        try {
//            String ds = ds.toUpperCase();
//            String hopo = hopo.toUpperCase();
//
//            int deleted = redisService.deleteDataInternal(ds, hopo);
//
//            return ResponseEntity.ok("✅ Deleted " + deleted + " key(s) for HOPO=" + hopo + ", DS=" + ds);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("❌ Failed to delete data: " + e.getMessage());
//        }
//    }
}
