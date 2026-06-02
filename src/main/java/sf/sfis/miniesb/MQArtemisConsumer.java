package sf.sfis.miniesb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import sf.sfis.miniesb.service.ESBResponseService;

@Slf4j
@Component
public class MQArtemisConsumer {

    @Autowired
    ESBResponseService receiverService;

    public void processMessage(Message message) {
        String queueName = "";
        try {
            if (message instanceof TextMessage textMessage) {
                if (message.getJMSDestination() instanceof Queue jmsQueue) {
                    queueName = jmsQueue.getQueueName(); // AQ_TO_FIDS_AOT_AOS_TST
                }
                log.info("Received XML from ActiveMQ [{}]", queueName);
                receiverService.convertXMLtoObject(textMessage.getText());
            } else {
                log.error("Received non-text message from ActiveMQ [{}]", queueName);
            }
        } catch (Exception e) {
            log.error("Error processing message from ActiveMQ [{}]" + queueName, e);
        }
    }

    @JmsListener(destination = "AQ_TO_FIDS_AOT_AOS_TST", containerFactory = "artemisContainerFactory")
    public void listenFidsTrigger(Message message) {
        processMessage(message);
    }

    @JmsListener(destination = "AQ_TO_AFTN_AOT_AOS_TST", containerFactory = "artemisContainerFactory")
    public void listenAftnTrigger(Message message) {
        processMessage(message);
    }

    @JmsListener(destination = "AQ_TO_SITA_AOT_AOS_TST", containerFactory = "artemisContainerFactory")
    public void listenSitaTrigger(Message message) {
        processMessage(message);
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
