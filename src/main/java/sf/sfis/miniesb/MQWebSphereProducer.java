package sf.sfis.miniesb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MQWebSphereProducer {
    // ยิงเข้าเครื่องพอร์ต 1414
    private final JmsTemplate mq1Template;
    // ยิงเข้าเครื่องพอร์ต 1415
    private final JmsTemplate mq2Template;

    public MQWebSphereProducer(@Qualifier("mq1JmsTemplate") JmsTemplate mq1Template,
            @Qualifier("mq2JmsTemplate") JmsTemplate mq2Template) {
        this.mq1Template = mq1Template;
        this.mq2Template = mq2Template;
    }

    public void sendToMachine1(String queue, String message) {
        log.info("Send XML to WebsphereMQ 1414 [{}]", queue);
        mq1Template.convertAndSend(queue, message);
    }

    public void sendToMachine2(String queue, String message) {
        log.info("Send XML to WebsphereMQ 1415 [{}]", queue);
        mq2Template.convertAndSend(queue, message);
    }
}