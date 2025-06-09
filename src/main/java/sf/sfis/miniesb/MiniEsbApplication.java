package sf.sfis.miniesb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableJms
public class MiniEsbApplication extends SpringBootServletInitializer {

	@Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;
    
    public static void main(String[] args) {
    	ApplicationContext context = SpringApplication.run(MiniEsbApplication.class, args);
        
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
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MiniEsbApplication.class);
    }
    
    @Bean
    public WebClient webClient() {
    	return WebClient.builder()
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
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
