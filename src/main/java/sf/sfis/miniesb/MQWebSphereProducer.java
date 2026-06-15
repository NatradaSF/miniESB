package sf.sfis.miniesb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Slf4j
@Component
public class MQWebSphereProducer {
    // ยิงเข้าเครื่องพอร์ต 1414
    private final JmsTemplate mq1Template;
    // ยิงเข้าเครื่องพอร์ต 1415
    private final JmsTemplate mq2Template;

    // เมื่อ false (WebSphere MQ ยังไม่พร้อม) จะ skip การส่ง แทนที่จะ throw error
    @Value("${websphere.mq.enabled:true}")
    private boolean enabled;

    // Dedicated logger for outbound XML to ESB → routed to logs/outbound/<hopo>/<queue>.log
    private static final Logger sendEsbLog = LoggerFactory.getLogger("SEND_ESB_XML");

    public MQWebSphereProducer(@Qualifier("mq1JmsTemplate") JmsTemplate mq1Template,
            @Qualifier("mq2JmsTemplate") JmsTemplate mq2Template) {
        this.mq1Template = mq1Template;
        this.mq2Template = mq2Template;
    }

    public void sendToMachine1(String queue, String hopo, String message) {
        if (!enabled) {
            log.warn("WebSphere MQ disabled (websphere.mq.enabled=false) — skip send to 1414 [{}]", queue);
            return;
        }
        log.info("Send XML to WebsphereMQ 1414 [{}]", queue);
        MDC.put("sendEsbKey", hopo + "/outbound-" + queue);
        try {
            sendEsbLog.info(message);
        } finally {
            MDC.remove("sendEsbKey");
        }
        mq1Template.convertAndSend(queue, message);
    }

    public void sendToMachine2(String queue, String hopo, String message) {
        if (!enabled) {
            log.warn("WebSphere MQ disabled (websphere.mq.enabled=false) — skip send to 1415 [{}]", queue);
            return;
        }
        log.info("Send XML to WebsphereMQ 1415 [{}]", queue);
        MDC.put("sendEsbKey", hopo + "/outbound-" + queue);
        try {
            sendEsbLog.info(message);
        } finally {
            MDC.remove("sendEsbKey");
        }
        mq2Template.convertAndSend(queue, message);
    }
}