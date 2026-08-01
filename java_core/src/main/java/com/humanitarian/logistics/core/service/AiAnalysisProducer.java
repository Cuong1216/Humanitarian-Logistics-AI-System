package com.humanitarian.logistics.core.service;

import com.humanitarian.logistics.core.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AiAnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    public AiAnalysisProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendToAiService(String postId) {
        Map<String, String> payload = new HashMap<>();
        payload.put("postId", postId);
        payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

        // Gửi vào Exchange, route đến Queue
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME, 
            RabbitMQConfig.ROUTING_KEY, 
            payload
        );
        
        log.info("Đã gửi postId: {} vào RabbitMQ (Queue: {})", postId, RabbitMQConfig.QUEUE_NAME);
    }
}
