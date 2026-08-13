package sf.sfis.ifimsconnect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import sf.sfis.ifimsconnect.service.ESBRequestService;

/**
 * WebSphere MQ inbound listeners. Disabled on environments where WebSphere MQ is
 * not yet available by setting {@code websphere.mq.enabled=false} — when disabled
 * this bean is not created, so no listener container starts and no connection is
 * attempted at startup. Defaults to enabled when the property is absent.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "websphere.mq.enabled", havingValue = "true", matchIfMissing = true)
public class MQWebSphereConsumer {

    @Autowired
    ESBRequestService receiverService;

    // Dedicated logger for inbound XML from ESB → routed to logs/receive_esb/<hopo>/<queue>.log
    private static final Logger receivedEsbLog = LoggerFactory.getLogger("RECEIVED_ESB_XML");

    private void processMessage(Message message, String queueName, String hopo) {
        /* String queueName = ""; */
        try {
            /* if (message.getJMSDestination() != null) {
                String destStr = message.getJMSDestination().toString();
                queueName = destStr.substring(destStr.lastIndexOf("/") + 1);
            } */
            String xmlContent = null;

            // 🟢 เคสที่ 1: ถ้าเป็น TextMessage ปกติ (อ่านตรงๆ)
            if (message instanceof TextMessage textMessage) {
                xmlContent = textMessage.getText();
                log.info("Received TextMessage from WebsphereMQ [{}]", queueName);

                // 🔵 เคสที่ 2: ถ้าเป็น BytesMessage (ท่าไม้ตายที่ webMethods ส่งมา)
            } else if (message instanceof BytesMessage bytesMessage) {
                log.info("Received BytesMessage from WebsphereMQ [{}], converting to String...", queueName);

                // อ่านขนาดของข้อมูลดิบทั้งหมด
                byte[] buffer = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(buffer);

                // แปลงไบนารีกลับมาเป็นตัวอักษรสากล (UTF-8) ตามมาตรฐานที่เราตั้งค่าตู้ไว้
                xmlContent = new String(buffer, java.nio.charset.StandardCharsets.UTF_8);
            }

            if (xmlContent != null) {
                MDC.put("recvEsbKey", hopo + "/inbound-" + queueName);
                try {
                    receivedEsbLog.info(xmlContent);
                } finally {
                    MDC.remove("recvEsbKey");
                }
                receiverService.processXmlMessage(xmlContent);
            } else {
                log.error("Received unsupported message type ({}) from WebsphereMQ [{}]",
                        message.getClass().getName(), queueName);
            }

            /* if (message instanceof TextMessage textMessage) {

                if (message.getJMSDestination() instanceof Queue jmsQueue) {
                    queueName = jmsQueue.getQueueName();
                }

                log.info("Received XML from WebsphereMQ [{}]", queueName);
                receiverService.processXmlMessage(textMessage.getText());
            } else {
                log.error("Received non-text message from WebsphereMQ [{}]", queueName);
            } */
        } catch (Exception e) {
            log.error("Error processing message from WebsphereMQ [{}]" + queueName, e);
        }
    }

    // --- Queue ของ BKK ---
    @JmsListener(destination = "UFIS_TRIGGER_IN_BKK", containerFactory = "mq1ContainerFactory")
    public void listenBkkTrigger(Message message) {
        processMessage(message, "UFIS_TRIGGER_IN_BKK", "BKK");
    }

    @JmsListener(destination = "UFIS_ATC_IN_BKK", containerFactory = "mq1ContainerFactory")
    public void listenBkkAtc(Message message) {
        processMessage(message, "UFIS_ATC_IN_BKK", "BKK");
    }

    @JmsListener(destination = "UFIS_OTHERS_IN_BKK", containerFactory = "mq1ContainerFactory")
    public void listenBkkOthers(Message message) {
        processMessage(message, "UFIS_OTHERS_IN_BKK", "BKK");
    }

    @JmsListener(destination = "UFIS_FIGURE_IN", containerFactory = "mq1ContainerFactory")
    public void listenBkkFigure(Message message) {
        processMessage(message, "UFIS_FIGURE_IN", "BKK");
    }

    @JmsListener(destination = "UFIS_MANIFEST_IN", containerFactory = "mq1ContainerFactory")
    public void listenBkkManifest(Message message) {
        processMessage(message, "UFIS_MANIFEST_IN", "BKK");
    }

    @JmsListener(destination = "UFIS_EQUIPMENT_IN", containerFactory = "mq1ContainerFactory")
    public void listenBkkEquipment(Message message) {
        processMessage(message, "UFIS_EQUIPMENT_IN", "BKK");
    }

    // --- Queue ของท่าอื่น ---
    @JmsListener(destination = "UFIS_TRIGGER_IN_DMK", containerFactory = "mq2ContainerFactory")
    public void listenOtherTriggerDmk(Message message) {
        processMessage(message, "UFIS_TRIGGER_IN_DMK", "DMK");
    }

    @JmsListener(destination = "UFIS_TRIGGER_IN_CNX", containerFactory = "mq2ContainerFactory")
    public void listenOtherTriggerCnx(Message message) {
        processMessage(message, "UFIS_TRIGGER_IN_CNX", "CNX");
    }

    @JmsListener(destination = "UFIS_TRIGGER_IN_CEI", containerFactory = "mq2ContainerFactory")
    public void listenOtherTriggerCei(Message message) {
        processMessage(message, "UFIS_TRIGGER_IN_CEI", "CEI");
    }

    @JmsListener(destination = "UFIS_TRIGGER_IN_HDY", containerFactory = "mq2ContainerFactory")
    public void listenOtherTriggerHdy(Message message) {
        processMessage(message, "UFIS_TRIGGER_IN_HDY", "HDY");
    }

    @JmsListener(destination = "UFIS_TRIGGER_IN_HKT", containerFactory = "mq2ContainerFactory")
    public void listenOtherTriggerHkt(Message message) {
        processMessage(message, "UFIS_TRIGGER_IN_HKT", "HKT");
    }

    @JmsListener(destination = "UFIS_ATC_IN_DMK", containerFactory = "mq2ContainerFactory")
    public void listenOtherAtcDmk(Message message) {
        processMessage(message, "UFIS_ATC_IN_DMK", "DMK");
    }

    @JmsListener(destination = "UFIS_ATC_IN_CNX", containerFactory = "mq2ContainerFactory")
    public void listenOtherAtcCnx(Message message) {
        processMessage(message, "UFIS_ATC_IN_CNX", "CNX");
    }

    @JmsListener(destination = "UFIS_ATC_IN_CEI", containerFactory = "mq2ContainerFactory")
    public void listenOtherAtcCei(Message message) {
        processMessage(message, "UFIS_ATC_IN_CEI", "CEI");
    }

    @JmsListener(destination = "UFIS_ATC_IN_HDY", containerFactory = "mq2ContainerFactory")
    public void listenOtherAtcHdy(Message message) {
        processMessage(message, "UFIS_ATC_IN_HDY", "HDY");
    }

    @JmsListener(destination = "UFIS_ATC_IN_HKT", containerFactory = "mq2ContainerFactory")
    public void listenOtherAtcHkt(Message message) {
        processMessage(message, "UFIS_ATC_IN_HKT", "HKT");
    }

    @JmsListener(destination = "UFIS_OTHERS_IN_DMK", containerFactory = "mq2ContainerFactory")
    public void listenOthersDmk(Message message) {
        processMessage(message, "UFIS_OTHERS_IN_DMK", "DMK");
    }

    @JmsListener(destination = "UFIS_OTHERS_IN_CNX", containerFactory = "mq2ContainerFactory")
    public void listenOthersCnx(Message message) {
        processMessage(message, "UFIS_OTHERS_IN_CNX", "CNX");
    }

    @JmsListener(destination = "UFIS_OTHERS_IN_CEI", containerFactory = "mq2ContainerFactory")
    public void listenOthersCei(Message message) {
        processMessage(message, "UFIS_OTHERS_IN_CEI", "CEI");
    }

    @JmsListener(destination = "UFIS_OTHERS_IN_HDY", containerFactory = "mq2ContainerFactory")
    public void listenOthersHdy(Message message) {
        processMessage(message, "UFIS_OTHERS_IN_HDY", "HDY");
    }

    @JmsListener(destination = "UFIS_OTHERS_IN_HKT", containerFactory = "mq2ContainerFactory")
    public void listenOthersHkt(Message message) {
        processMessage(message, "UFIS_OTHERS_IN_HKT", "HKT");
    }
}
