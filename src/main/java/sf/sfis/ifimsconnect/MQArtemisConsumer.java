package sf.sfis.ifimsconnect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sf.sfis.ifimsconnect.service.ESBResponseService;

@Slf4j
@Component
public class MQArtemisConsumer {

    @Autowired
    ESBResponseService receiverService;

    // Dedicated logger for raw inbound XML payloads → routed to logs/received.log
    // only
    // (additivity=false in logback-spring.xml), keeping bulky XML out of app.log.
    private static final Logger receivedLog = LoggerFactory.getLogger("RECEIVED_XML");

    public void processMessage(Message message, String queueName, String hopo) {
        try {
            if (message instanceof TextMessage textMessage) {
                String xml = textMessage.getText();
                log.info("Received XML from ActiveMQ [{}]", queueName);

                // แยกไฟล์ log payload ตาม hopo + queue ผ่าน MDC
                // (SiftingAppender ใน logback-spring.xml จะอ่านค่า key "recvKey" นี้ไปตั้งชื่อไฟล์)
                MDC.put("recvKey", hopo + "/receive-" + queueName);
                try {
                    receivedLog.info(xml);
                } finally {
                    MDC.remove("recvKey"); // กัน MDC ค้างใน thread (listener container ใช้ thread ซ้ำ)
                }

                receiverService.convertXMLtoObject(xml);
            } else {
                log.error("Received non-text message from ActiveMQ [{}]", queueName);
            }
        } catch (Exception e) {
            log.error("Error processing message from ActiveMQ [{}]" + queueName, e);
        }
    }

    @JmsListener(destination = "AQ_TO_FIDS_AOT_AOS_TST", containerFactory = "artemisContainerFactory")
    public void listenFidsTriggerBKK(Message message) {
        processMessage(message, "AQ_TO_FIDS_AOT_AOS_TST", "BKK");
    }

    @JmsListener(destination = "AQ_TO_AFTN_AOT_AOS_TST", containerFactory = "artemisContainerFactory")
    public void listenAftnTrigger(Message message) {
        processMessage(message, "AQ_TO_AFTN_AOT_AOS_TST", "BKK");
    }

    @JmsListener(destination = "AQ_TO_SITA_AOT_AOS_TST", containerFactory = "artemisContainerFactory")
    public void listenSitaTrigger(Message message) {
        processMessage(message, "AQ_TO_SITA_AOT_AOS_TST", "BKK");
    }

    /*
     * @JmsListener(destination = "AQ_TO_AFTN_AOT_AOS_TST", containerFactory =
     * "artemisContainerFactory")
     * public void listenQueueAftn(Message message) {
     * try {
     * if (message instanceof TextMessage textMessage) {
     * // System.out.println("📥 [AFTN] Received: " + textMessage.getText());
     * receiverService.convertXMLtoObject(textMessage.getText());
     * } else {
     * log.error("Received non-text message");
     * }
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     * 
     * @JmsListener(destination = "AQ_TO_SITA_AOT_AOS_TST", containerFactory =
     * "artemisContainerFactory")
     * public void listenQueueSita(Message message) {
     * try {
     * if (message instanceof TextMessage textMessage) {
     * // log.info("📥 [SITA] Received: " + textMessage.getText());
     * receiverService.convertXMLtoObject(textMessage.getText());
     * } else {
     * log.error("Received non-text message");
     * }
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     */

    // @JmsListener(destination = "AQ_MQIFINT_UPDATE_AOT_AOS_TST", containerFactory
    // = "artemisContainerFactory")
    // public void listenQueueAsyncUpdate(Message message) {
    // try {
    // if (message instanceof TextMessage textMessage) {
    // System.out.println("📥 [Async UPDATE] Received: " + textMessage.getText());
    // } else {
    // System.out.println("Received non-text message");
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
}
