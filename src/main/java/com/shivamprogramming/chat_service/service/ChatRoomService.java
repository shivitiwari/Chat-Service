package com.shivamprogramming.chat_service.service;

import com.shivamprogramming.chat_service.model.ChatRoom;
import com.shivamprogramming.chat_service.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ChatRoomService {

    private static final String ROOM_MEMBERS_PREFIX = "chat:room:members:";
    private static final String USER_ROOMS_PREFIX = "chat:user:rooms:";

    private final ChatRoomRepository chatRoomRepository;
    private final StringRedisTemplate redisTemplate;

    public ChatRoomService(ChatRoomRepository chatRoomRepository,
                           StringRedisTemplate redisTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Create a new chat room. Saved to MongoDB.
     */
    public ChatRoom createRoom(String name, String description, String createdBy) {
        if (chatRoomRepository.existsByName(name)) {
            return chatRoomRepository.findByName(name).orElse(null);
        }
        ChatRoom room = ChatRoom.builder()
                .name(name)
                .description(description)
                .createdBy(createdBy)
                .build();
        ChatRoom saved = chatRoomRepository.save(room);
        log.info("Room created: {} by {}", name, createdBy);
        return saved;
    }

    /**
     * Add a user to a room (tracked in Redis).
     */
    public void joinRoom(String roomName, String username) {
        try {
            redisTemplate.opsForSet().add(ROOM_MEMBERS_PREFIX + roomName, username);
            redisTemplate.opsForSet().add(USER_ROOMS_PREFIX + username, roomName);
            log.info("User {} joined room {}", username, roomName);
        } catch (Exception e) {
            log.warn("Redis unavailable for joinRoom. error={}", e.getMessage());
        }
    }

    /**
     * Remove a user from a room (tracked in Redis).
     */
    public void leaveRoom(String roomName, String username) {
        try {
            redisTemplate.opsForSet().remove(ROOM_MEMBERS_PREFIX + roomName, username);
            redisTemplate.opsForSet().remove(USER_ROOMS_PREFIX + username, roomName);
            log.info("User {} left room {}", username, roomName);
        } catch (Exception e) {
            log.warn("Redis unavailable for leaveRoom. error={}", e.getMessage());
        }
    }

    /**
     * Remove a user from ALL rooms (called on disconnect).
     */
    public Set<String> leaveAllRooms(String username) {
        Set<String> rooms = getUserRooms(username);
        for (String room : rooms) {
            leaveRoom(room, username);
        }
        return rooms;
    }

    /**
     * Get all members currently in a room.
     */
    public Set<String> getRoomMembers(String roomName) {
        try {
            Set<String> members = redisTemplate.opsForSet().members(ROOM_MEMBERS_PREFIX + roomName);
            return members != null ? members : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * Get all rooms a user has joined.
     */
    public Set<String> getUserRooms(String username) {
        try {
            Set<String> rooms = redisTemplate.opsForSet().members(USER_ROOMS_PREFIX + username);
            return rooms != null ? rooms : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    /**
     * Get all rooms from MongoDB.
     */
    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }
}