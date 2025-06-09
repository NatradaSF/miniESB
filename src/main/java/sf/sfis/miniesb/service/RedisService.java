package sf.sfis.miniesb.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisService.class);
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

//    // ฟังก์ชันลบข้อมูล Redis ภายใน
//    public int deleteDataInternal(String ds, String hopo) {
//        String pattern = hopo + "/" + ds;
//        Set<String> keys = redisTemplate.keys(pattern);
//        int deleted = 0;
//        if (keys != null && !keys.isEmpty()) {
//            redisTemplate.delete(keys);
//            deleted = keys.size();
//        }
//        return deleted;
//    }

    // ฟังก์ชันบันทึกข้อมูลลง Redis
    public String saveDataToRedis(List<Map<String, Object>> dataList, String ds, String hopo) {
        try {
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withZone(ZoneId.of("Asia/Bangkok"))
                    .format(Instant.now());

            String key = hopo + "/" + ds;
            String keyTime = hopo + "/timestamp";

            String jsonData = objectMapper.writeValueAsString(dataList);

            redisTemplate.opsForValue().set(key, jsonData);
            redisTemplate.opsForValue().set(keyTime, timestamp);

            return "Saved with key: " + key + " and "+keyTime+" : "+timestamp;

        } catch (Exception e) {
        	LOGGER.error("saveDataToRedis: ", e);
            return "Failed to save: " + e.getMessage();
        }
    }
}
