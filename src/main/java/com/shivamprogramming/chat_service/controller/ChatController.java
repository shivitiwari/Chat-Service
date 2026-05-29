package com.shivamprogramming.chat_service.controller;

import com.shivamprogramming.chat_service.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles public messages broadcast to all subscribers of /topic/public
     * Client sends to : /app/chat.sendMessage
     * Delivered to    : /topic/public
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        return chatMessage;
    }

    /**
     * Handles a user joining the chat room
     * Client sends to : /app/chat.addUser
     * Delivered to    : /topic/public
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Store username in WebSocket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderId());
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getSenderId() + " joined the chat!");
        return chatMessage;
    }

    /**
     * Handles private/direct messages between two users
     * Client sends to : /app/chat.privateMessage
     * Delivered to    : /user/{recipientId}/queue/private
     */
    @MessageMapping("/chat.privateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.CHAT);

        // Send to a specific user's private queue
        messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipientId(),
                "/queue/private",
                chatMessage
        );
    }
}