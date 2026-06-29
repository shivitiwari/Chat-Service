package com.shivamprogramming.chat_service.kafka;

import com.shivamprogramming.chat_service.dto.ChatMessageEvent;
import com.shivamprogramming.chat_service.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, ChatMessageEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderEvent(ChatMessage message) {
        try {
            ChatMessageEvent event = new ChatMessageEvent(
                    message.getId(),
                    message.getSenderId(),
                    message.getRecipientId(),
                    message.getContent(),
                    message.getTimestamp(),
                    message.getType());

            kafkaTemplate.send("message-events", event.getSenderId(), event);
            log.info("Message event sent to Kafka. sender={}", message.getSenderId());
        } catch (Exception e) {
            log.warn("Kafka unavailable, skipping persistence. error={}", e.getMessage());
        }
    }
}
