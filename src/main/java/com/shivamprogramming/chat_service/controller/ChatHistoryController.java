package com.shivamprogramming.chat_service.controller;

import com.shivamprogramming.chat_service.model.ChatMessage;
import com.shivamprogramming.chat_service.model.ChatRoom;
import com.shivamprogramming.chat_service.repository.ChatMessageRepository;
import com.shivamprogramming.chat_service.service.ChatRoomService;
import com.shivamprogramming.chat_service.service.OnlineUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatMessageRepository chatMessageRepository;
    private final OnlineUserService onlineUserService;
    private final ChatRoomService chatRoomService;

    public ChatHistoryController(ChatMessageRepository chatMessageRepository,
                                 OnlineUserService onlineUserService,
                                 ChatRoomService chatRoomService) {
        this.chatMessageRepository = chatMessageRepository;
        this.onlineUserService = onlineUserService;
        this.chatRoomService = chatRoomService;
    }

    // ── Chat History ──────────────────────────────────────────

    @GetMapping("/history/{roomId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String roomId) {
        return ResponseEntity.ok(chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId));
    }

    @GetMapping("/history/{roomId}/recent")
    public ResponseEntity<List<ChatMessage>> getRecentMessages(@PathVariable String roomId) {
        return ResponseEntity.ok(chatMessageRepository.findTop50ByRoomIdOrderByTimestampDesc(roomId));
    }

    @GetMapping("/unread/{recipientId}")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String recipientId) {
        return ResponseEntity.ok(chatMessageRepository.countByRecipientIdAndDeliveredFalse(recipientId));
    }

    // ── Online Users ──────────────────────────────────────────

    @GetMapping("/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(onlineUserService.getOnlineUsers());
    }

    @GetMapping("/online-users/count")
    public ResponseEntity<Map<String, Long>> getOnlineUserCount() {
        return ResponseEntity.ok(Map.of("count", onlineUserService.getOnlineCount()));
    }

    @GetMapping("/online-users/{username}/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable String username) {
        return ResponseEntity.ok(Map.of("username", username, "online", onlineUserService.isUserOnline(username)));
    }

    // ── Chat Rooms ────────────────────────────────────────────

    /**
     * GET /api/chat/rooms — list all rooms
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getAllRooms() {
        return ResponseEntity.ok(chatRoomService.getAllRooms());
    }

    /**
     * POST /api/chat/rooms — create a new room
     * Body: { "name": "general", "description": "General chat", "createdBy": "alice" }
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody Map<String, String> body) {
        ChatRoom room = chatRoomService.createRoom(
                body.get("name"),
                body.getOrDefault("description", ""),
                body.getOrDefault("createdBy", "system")
        );
        return ResponseEntity.ok(room);
    }

    /**
     * GET /api/chat/rooms/{roomName}/members — get members in a room
     */
    @GetMapping("/rooms/{roomName}/members")
    public ResponseEntity<Set<String>> getRoomMembers(@PathVariable String roomName) {
        return ResponseEntity.ok(chatRoomService.getRoomMembers(roomName));
    }

    /**
     * GET /api/chat/rooms/user/{username} — get all rooms a user has joined
     */
    @GetMapping("/rooms/user/{username}")
    public ResponseEntity<Set<String>> getUserRooms(@PathVariable String username) {
        return ResponseEntity.ok(chatRoomService.getUserRooms(username));
    }
}
