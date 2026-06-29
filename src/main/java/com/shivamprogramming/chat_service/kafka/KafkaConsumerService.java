package com.shivamprogramming.chat_service.kafka;

import com.shivamprogramming.chat_service.dto.ChatMessageEvent;
import com.shivamprogramming.chat_service.model.ChatMessage;
import com.shivamprogramming.chat_service.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    private final ChatMessageRepository chatMessageRepository;

    public KafkaConsumerService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * Listens on Kafka topic "message-events"
     * Saves ChatMessage document to MongoDB collection "messages"
     */
    @KafkaListener(
            topics           = "${kafka.topic.message-events}",
            groupId          = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessageEvent(ConsumerRecord<String, ChatMessageEvent> record) {

        ChatMessageEvent event = record.value();

        log.info("Kafka message received | topic={} | partition={} | offset={} | key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            ChatMessage chatMessage = ChatMessage.builder()
                    .id(event.getId())
                    .senderId(event.getSenderId())
                    .recipientId(event.getRecipientId())
                    .content(event.getContent())
                    .timestamp(event.getTimestamp())
                    .type(event.getType())
                    .roomId(resolveRoomId(event))
                    .delivered(true)
                    .build();

            ChatMessage saved = chatMessageRepository.save(chatMessage);

            log.info("Message saved to MongoDB | id={} | roomId={} | type={}",
                    saved.getId(), saved.getRoomId(), saved.getType());

        } catch (Exception e) {
            log.error("Failed to save message to MongoDB | messageId={} | error={}",
                    event.getId(), e.getMessage(), e);
        }
    }

    /**
     * Resolves roomId:
     * JOIN / LEAVE       -> "public"
     * Public CHAT        -> "public"
     * Private CHAT (DM)  -> sorted "alice-bob" (same regardless of who queries)
     */
    private String resolveRoomId(ChatMessageEvent event) {
        if (event.getType() == ChatMessage.MessageType.JOIN ||
            event.getType() == ChatMessage.MessageType.LEAVE) {
            return "public";
        }
        if (event.getRecipientId() != null && !event.getRecipientId().isBlank()
                && !event.getRecipientId().equals(event.getSenderId())) {
            String a = event.getSenderId();
            String b = event.getRecipientId();
            return a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
        }
        return "public";
    }
}

