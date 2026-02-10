package org.ozonehis.eip.hcw.config;

import org.apache.camel.component.springrabbit.SpringRabbitMQComponent;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.port}")
    private int port;

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean(name = "spring-rabbitmq")
    public SpringRabbitMQComponent springRabbitmq(ConnectionFactory connectionFactory) {
        SpringRabbitMQComponent component = new SpringRabbitMQComponent();
        component.setConnectionFactory(connectionFactory);
        return component;
    }
}
