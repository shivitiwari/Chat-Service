package com.shivamprogramming.chat_service.config;

import com.shivamprogramming.chat_service.dto.ChatMessageEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.listener.auto-startup:true}")
    private boolean autoStartup;

    @Bean
    public ConsumerFactory<String, ChatMessageEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        // ── Broker address ─────────────────────────────────────────
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // ── Consumer group ─────────────────────────────────────────
        // All consumers in same group share partitions (load balanced)
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // ── Offset reset ───────────────────────────────────────────
        // "earliest" → read from beginning if no committed offset found
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // ── Deserializers ──────────────────────────────────────────
        // Wrapped in ErrorHandlingDeserializer to prevent poison pill (bad message crashing consumer)
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,   StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // ── JSON Deserializer settings ─────────────────────────────
        // Trust our own package to deserialize
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.shivamprogramming.chat_service.*");

        // Type mapping — producer sends "chatMessage", consumer maps it to ChatMessageEvent
        config.put(JsonDeserializer.TYPE_MAPPINGS,
                "chatMessage:com.shivamprogramming.chat_service.dto.ChatMessageEvent");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, ChatMessageEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Process up to 3 partitions concurrently
        factory.setConcurrency(3);

        // Respect spring.kafka.listener.auto-startup property
        factory.setAutoStartup(autoStartup);

        return factory;
    }
}

