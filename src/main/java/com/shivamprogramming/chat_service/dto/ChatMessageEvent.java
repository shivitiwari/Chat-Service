package com.shivamprogramming.chat_service.dto;

import com.shivamprogramming.chat_service.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatMessageEvent {
    private String id;
    private String senderId;
    private String recipientId;
    private String content;
    private LocalDateTime timestamp;
    private ChatMessage.MessageType type; // "CHAT", "JOIN", "LEAVE"
}
