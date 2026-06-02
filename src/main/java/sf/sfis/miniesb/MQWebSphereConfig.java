package sf.sfis.miniesb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;

import jakarta.jms.ConnectionFactory;

@Configuration
public class MQWebSphereConfig {
    // ==========================================
    // ชุดที่ 1: สำหรับเครื่อง Port 1414 (mq1)
    // ==========================================
    @Bean(name = "mq1ConnectionFactory")
    public MQConnectionFactory mq1ConnectionFactory(
            @Value("${ibm.mq.mq1.queue-manager}") String qmgr,
            @Value("${ibm.mq.mq1.channel}") String channel,
            @Value("${ibm.mq.mq1.conn-name}") String connName,
            @Value("${ibm.mq.mq1.user}") String user,
            @Value("${ibm.mq.mq1.password}") String password) throws Exception {

        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setQueueManager(qmgr);
        factory.setChannel(channel);
        factory.setConnectionNameList(connName);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setStringProperty(WMQConstants.USERID, user);
        factory.setStringProperty(WMQConstants.PASSWORD, password);
        return factory;
    }

    @Bean(name = "mq1ContainerFactory")
    public DefaultJmsListenerContainerFactory mq1ContainerFactory(
            @Qualifier("mq1ConnectionFactory") ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    @Bean(name = "mq1JmsTemplate")
    public JmsTemplate mq1JmsTemplate(@Qualifier("mq1ConnectionFactory") ConnectionFactory cf) {
        return new JmsTemplate(cf);
    }

    // ==========================================
    // ชุดที่ 2: สำหรับเครื่อง Port 1415 (mq2)
    // ==========================================
    @Bean(name = "mq2ConnectionFactory")
    public MQConnectionFactory mq2ConnectionFactory(
            @Value("${ibm.mq.mq2.queue-manager}") String qmgr,
            @Value("${ibm.mq.mq2.channel}") String channel,
            @Value("${ibm.mq.mq2.conn-name}") String connName,
            @Value("${ibm.mq.mq2.user}") String user,
            @Value("${ibm.mq.mq2.password}") String password) throws Exception {

        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setQueueManager(qmgr);
        factory.setChannel(channel);
        factory.setConnectionNameList(connName);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setStringProperty(WMQConstants.USERID, user);
        factory.setStringProperty(WMQConstants.PASSWORD, password);
        return factory;
    }

    @Bean(name = "mq2ContainerFactory")
    public DefaultJmsListenerContainerFactory mq2ContainerFactory(
            @Qualifier("mq2ConnectionFactory") ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    @Bean(name = "mq2JmsTemplate")
    public JmsTemplate mq2JmsTemplate(@Qualifier("mq2ConnectionFactory") ConnectionFactory cf) {
        return new JmsTemplate(cf);
    }
}
