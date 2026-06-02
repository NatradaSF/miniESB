package sf.sfis.miniesb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MQArtemisProducer {
    private final JmsTemplate jmsTemplate;

    public MQArtemisProducer(@Qualifier("artemisJmsTemplate") JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendMessage(String queue, String message) {
        log.info("Send XML to ActiveMQ [{}]", queue);
        jmsTemplate.convertAndSend(queue, message);
        // System.out.println("✅ ส่งข้อความ: " + message);
    }
}