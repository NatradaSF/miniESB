package sf.sfis.ifimsconnect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableJms
@EnableScheduling
@RequiredArgsConstructor
public class IFimsConnectApplication extends SpringBootServletInitializer {

	@Value("${spring.redis.host}")
	private String redisHost;

	@Value("${spring.redis.port}")
	private int redisPort;

	private static ObjectMapper objectMapper = new ObjectMapper();

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(IFimsConnectApplication.class, args);

//        File xmlFile = new File("update_new.xml");
//		String xmlContent;
//		try {
//			xmlContent = new String(Files.readAllBytes(Paths.get(xmlFile.getPath())), "UTF-8");
//			ESBResponseService esbResponseService = context.getBean(ESBResponseService.class);
//			esbResponseService.convertXMLtoObject(xmlContent);
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

//    	
//    	String ds = "DEP";
//        String hopo = "BKK";
////
////        // เรียกฟังก์ชัน saveDataToRedis
////        RedisService redisService = context.getBean(RedisService.class);
////        String result = redisService.saveDataToRedis(dataList, ds, hopo);
//
//        RedisController redisController = context.getBean(RedisController.class);
//        redisController.getTimestamp(hopo);
//        redisController.getData(ds, hopo);
		
//		SubscribeScheduledService subscribeAndRequest = context.getBean(SubscribeScheduledService.class);
//		subscribeAndRequest.subscribeAndRequestAfttab();
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(IFimsConnectApplication.class);
	}

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory(redisHost, redisPort);
	}
	
//    @Bean
//    public CommandLineRunner checkRedisConfig(RedisConnectionFactory factory) {
//    	System.out.println("=======================");
//        return args -> {
//            System.out.println("🌟 Redis ConnectionFactory class: " + factory.getClass().getName());
//            // ถ้าใช้ LettuceConnectionFactory ลอง cast ดู host, port
//            if (factory instanceof LettuceConnectionFactory lettuce) {
//                System.out.println("🌟 Lettuce Redis Host: " + lettuce.getHostName());
//                System.out.println("🌟 Lettuce Redis Port: " + lettuce.getPort());
//            }
//        };
//    }
}
