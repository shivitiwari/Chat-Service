package com.shivamprogramming.chat_service.listener;

import com.shivamprogramming.chat_service.model.ChatMessage;
import com.shivamprogramming.chat_service.service.ChatRoomService;
import com.shivamprogramming.chat_service.service.OnlineUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
public class WebSocketEventListener {

    private final OnlineUserService onlineUserService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(OnlineUserService onlineUserService,
                                  ChatRoomService chatRoomService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.onlineUserService = onlineUserService;
        this.chatRoomService = chatRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("WebSocket connected. sessionId={}",
                StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            log.info("User disconnected: {}", username);

            // ── Remove from global online set ─────────────────────
            onlineUserService.removeUser(username);

            // ── Leave all rooms and broadcast LEAVE to each ───────
            Set<String> rooms = chatRoomService.leaveAllRooms(username);
            for (String roomName : rooms) {
                ChatMessage leaveMsg = ChatMessage.builder()
                        .senderId(username)
                        .content(username + " left room #" + roomName)
                        .type(ChatMessage.MessageType.LEAVE)
                        .roomId(roomName)
                        .timestamp(LocalDateTime.now())
                        .build();
                messagingTemplate.convertAndSend("/topic/room/" + roomName, leaveMsg);

                Set<String> members = chatRoomService.getRoomMembers(roomName);
                messagingTemplate.convertAndSend("/topic/room/" + roomName + "/members", members);
            }

            // ── Broadcast LEAVE to public room ────────────────────
            ChatMessage publicLeave = ChatMessage.builder()
                    .senderId(username)
                    .content(username + " left the chat")
                    .type(ChatMessage.MessageType.LEAVE)
                    .roomId("public")
                    .timestamp(LocalDateTime.now())
                    .build();
            messagingTemplate.convertAndSend("/topic/public", publicLeave);

            broadcastOnlineUsers();
        }
    }

    public void broadcastOnlineUsers() {
        Set<String> onlineUsers = onlineUserService.getOnlineUsers();
        messagingTemplate.convertAndSend("/topic/online-users", onlineUsers);
    }
}
