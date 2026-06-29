package com.shivamprogramming.chat_service.controller;

import com.shivamprogramming.chat_service.model.ChatMessage;
import com.shivamprogramming.chat_service.kafka.KafkaProducerService;
import com.shivamprogramming.chat_service.service.ChatRoomService;
import com.shivamprogramming.chat_service.service.OnlineUserService;
import com.shivamprogramming.chat_service.listener.WebSocketEventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Set;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaProducerService kafkaProducerService;
    private final OnlineUserService onlineUserService;
    private final ChatRoomService chatRoomService;
    private final WebSocketEventListener webSocketEventListener;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          KafkaProducerService kafkaProducerService,
                          OnlineUserService onlineUserService,
                          ChatRoomService chatRoomService,
                          WebSocketEventListener webSocketEventListener) {
        this.messagingTemplate = messagingTemplate;
        this.kafkaProducerService = kafkaProducerService;
        this.onlineUserService = onlineUserService;
        this.chatRoomService = chatRoomService;
        this.webSocketEventListener = webSocketEventListener;
    }

    // ══════════════════════════════════════════════════════════
    //  PUBLIC ROOM (default)
    // ══════════════════════════════════════════════════════════

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        chatMessage.setRoomId("public");

        kafkaProducerService.sendOrderEvent(chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderId());
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent(chatMessage.getSenderId() + " joined the chat!");
        chatMessage.setRoomId("public");

        onlineUserService.addUser(chatMessage.getSenderId());
        chatRoomService.joinRoom("public", chatMessage.getSenderId());
        webSocketEventListener.broadcastOnlineUsers();

        kafkaProducerService.sendOrderEvent(chatMessage);
        return chatMessage;
    }

    // ══════════════════════════════════════════════════════════
    //  NAMED ROOMS — /app/chat.room.join, .leave, .send
    // ══════════════════════════════════════════════════════════

    /**
     * Join a named room.
     * Client sends to: /app/chat.room.join.{roomName}
     * Broadcast to   : /topic/room/{roomName}
     */
    @MessageMapping("/chat.room.join.{roomName}")
    public void joinRoom(@DestinationVariable String roomName,
                         @Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setRoomId(roomName);
        chatMessage.setContent(chatMessage.getSenderId() + " joined room #" + roomName);

        // Create room in MongoDB if it doesn't exist
        chatRoomService.createRoom(roomName, "", chatMessage.getSenderId());

        // Track membership in Redis
        chatRoomService.joinRoom(roomName, chatMessage.getSenderId());

        // Broadcast JOIN to room subscribers
        messagingTemplate.convertAndSend("/topic/room/" + roomName, chatMessage);

        // Broadcast updated room members
        Set<String> members = chatRoomService.getRoomMembers(roomName);
        messagingTemplate.convertAndSend("/topic/room/" + roomName + "/members", members);

        kafkaProducerService.sendOrderEvent(chatMessage);
    }

    /**
     * Leave a named room.
     * Client sends to: /app/chat.room.leave.{roomName}
     * Broadcast to   : /topic/room/{roomName}
     */
    @MessageMapping("/chat.room.leave.{roomName}")
    public void leaveRoom(@DestinationVariable String roomName,
                          @Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.LEAVE);
        chatMessage.setRoomId(roomName);
        chatMessage.setContent(chatMessage.getSenderId() + " left room #" + roomName);

        chatRoomService.leaveRoom(roomName, chatMessage.getSenderId());

        messagingTemplate.convertAndSend("/topic/room/" + roomName, chatMessage);

        Set<String> members = chatRoomService.getRoomMembers(roomName);
        messagingTemplate.convertAndSend("/topic/room/" + roomName + "/members", members);

        kafkaProducerService.sendOrderEvent(chatMessage);
    }

    /**
     * Send a message to a named room.
     * Client sends to: /app/chat.room.send.{roomName}
     * Broadcast to   : /topic/room/{roomName}
     */
    @MessageMapping("/chat.room.send.{roomName}")
    public void sendRoomMessage(@DestinationVariable String roomName,
                                @Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        chatMessage.setRoomId(roomName);

        messagingTemplate.convertAndSend("/topic/room/" + roomName, chatMessage);

        kafkaProducerService.sendOrderEvent(chatMessage);
    }

    // ══════════════════════════════════════════════════════════
    //  PRIVATE DM
    // ══════════════════════════════════════════════════════════

    @MessageMapping("/chat.privateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setType(ChatMessage.MessageType.CHAT);

        messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipientId(),
                "/queue/private",
                chatMessage
        );
        kafkaProducerService.sendOrderEvent(chatMessage);
    }
}