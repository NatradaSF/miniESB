package sf.sfis.miniesb;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ArtemisProducer {
	private final JmsTemplate jmsTemplate;

    public void sendMessage(String queuename, String message) {
    	jmsTemplate.convertAndSend(queuename, message);
//        System.out.println("✅ ส่งข้อความ: " + message);
    }
}