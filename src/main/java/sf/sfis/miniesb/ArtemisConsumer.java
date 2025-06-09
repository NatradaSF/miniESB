package sf.sfis.miniesb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import sf.sfis.miniesb.service.ESBResponseService;

@Component
public class ArtemisConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisConsumer.class);
	
	@Autowired
	ESBResponseService receiverService;
	
    @JmsListener(destination = "AQ_TO_FIDS_AOT_AOS_TST")
    public void listenQueueFids(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
//            	LOGGER.info("📥 [FIDS] Received: " + textMessage.getText());
//            	SubscribeResponseService receiverService = new SubscribeResponseService();
//            	receiverService.convertXMLtoObject(textMessage.getText());
//            	ESBResponseService receiverService = new ESBResponseService();
            	receiverService.convertXMLtoObject(textMessage.getText());
            } else {
                System.out.println("Received non-text message");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @JmsListener(destination = "AQ_TO_AFTN_AOT_AOS_TST")
    public void listenQueueAftn(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
//                System.out.println("📥 [AFTN] Received: " + textMessage.getText());
//            	ESBResponseService receiverService = new ESBResponseService();
            	receiverService.convertXMLtoObject(textMessage.getText());
            } else {
                System.out.println("Received non-text message");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @JmsListener(destination = "AQ_TO_SITA_AOT_AOS_TST")
    public void listenQueueSita(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                System.out.println("📥 [SITA] Received: " + textMessage.getText());
////            	ESBResponseService receiverService = new ESBResponseService();
//            	receiverService.convertXMLtoObject(textMessage.getText());
            } else {
                System.out.println("Received non-text message");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @JmsListener(destination = "AQ_MQIFINT_UPDATE_AOT_AOS_TST")
//    public void listenQueueAsyncUpdate(Message message) {
//        try {
//            if (message instanceof TextMessage textMessage) {
//                System.out.println("📥 [Async UPDATE] Received: " + textMessage.getText());
//            } else {
//                System.out.println("Received non-text message");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
