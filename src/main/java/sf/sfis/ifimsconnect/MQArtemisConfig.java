package sf.sfis.ifimsconnect;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;

@Configuration
public class MQArtemisConfig {
    @Value("${spring.artemis.broker-url}")
    private String brokerUrl;

    @Value("${spring.artemis.user:#{null}}")
    private String user;

    @Value("${spring.artemis.password:#{null}}")
    private String password;

    @Bean(name = "artemisConnectionFactory")
    public ConnectionFactory artemisConnectionFactory() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        if (user != null) {
            factory.setUser(user);
            factory.setPassword(password);
        }
        return factory;
    }

    @Bean(name = "artemisContainerFactory")
    public DefaultJmsListenerContainerFactory artemisContainerFactory(
            @Qualifier("artemisConnectionFactory") ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        return factory;
    }

    @Bean(name = "artemisJmsTemplate")
    public JmsTemplate artemisJmsTemplate(@Qualifier("artemisConnectionFactory") ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }
}
