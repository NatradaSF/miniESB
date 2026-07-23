package sf.sfis.ifimsconnect;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Slf4j
@Component
public class MQArtemisProducer {
    private final JmsTemplate jmsTemplate;
    private static final Logger sendLog = LoggerFactory.getLogger("SEND_XML");

    public MQArtemisProducer(@Qualifier("artemisJmsTemplate") JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void sendMessage(String queue, String hopo, String message) {
        log.info("Send XML to ActiveMQ [{}]", queue);
        MDC.put("sendKey", hopo + "/send-" + queue);
        try {
            sendLog.info(message);
        } finally {
            MDC.remove("sendKey"); // กัน MDC ค้างใน thread (listener container ใช้ thread ซ้ำ)
        }
        jmsTemplate.convertAndSend(queue, message);
        // System.out.println("✅ ส่งข้อความ: " + message);
    }
}