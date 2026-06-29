package com.shivamprogramming.chat_service.config;

import com.shivamprogramming.chat_service.dto.ChatMessageEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, ChatMessageEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // ── Kafka broker address ───────────────────────────────────
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // ── Serializers ────────────────────────────────────────────
        // Key   → plain String  (e.g. senderId)
        // Value → JSON (ChatMessage serialized to JSON bytes)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // ── Reliability settings ───────────────────────────────────
        // "all" → wait for leader + all in-sync replicas to acknowledge
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry up to 3 times on transient failures
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Max time to block on send() when broker is unavailable (default 60s → reduced to 3s)
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);

        // ── Performance settings ───────────────────────────────────
        // Batch messages up to 16 KB before sending
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // Wait up to 1ms to fill a batch (reduces requests)
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);

        // 32 MB send buffer
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ChatMessageEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

