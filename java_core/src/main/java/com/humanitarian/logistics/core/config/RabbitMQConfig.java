package com.humanitarian.logistics.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ai.analysis.exchange";
    public static final String QUEUE_NAME = "ai.analysis.queue";
    public static final String ROUTING_KEY = "ai.analysis.routing";

    public static final String DLX_NAME = "ai.dlx";
    public static final String DLQ_NAME = "ai.analysis.dlq";
    public static final String DLQ_ROUTING_KEY = "ai.dlq.routing";

    // 1. Định nghĩa Dead Letter Exchange & Queue
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    // 2. Định nghĩa Main Exchange & Queue (gắn DLX vào Main Queue)
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME) // Trỏ tới DLX khi message bị reject
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue()).to(mainExchange()).with(ROUTING_KEY);
    }

    // 3. Chuyển đổi Object thành JSON khi gửi
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
