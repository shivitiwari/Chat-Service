package com.shivamprogramming.chat_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Service
public class OnlineUserService {

    private static final String ONLINE_USERS_KEY = "chat:online-users";

    private final StringRedisTemplate redisTemplate;

    public OnlineUserService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Mark a user as online (add to Redis Set).
     */
    public void addUser(String username) {
        try {
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);
            log.info("User online: {} | total={}", username, getOnlineCount());
        } catch (Exception e) {
            log.warn("Redis unavailable, cannot track online user. error={}", e.getMessage());
        }
    }

    /**
     * Mark a user as offline (remove from Redis Set).
     */
    public void removeUser(String username) {
        try {
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);
            log.info("User offline: {} | total={}", username, getOnlineCount());
        } catch (Exception e) {
            log.warn("Redis unavailable, cannot remove online user. error={}", e.getMessage());
        }
    }

    /**
     * Get all currently online users.
     */
    public Set<String> getOnlineUsers() {
        try {
            Set<String> users = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            return users != null ? users : Collections.emptySet();
        } catch (Exception e) {
            log.warn("Redis unavailable, returning empty set. error={}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Get count of online users.
     */
    public long getOnlineCount() {
        try {
            Long size = redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if a specific user is online.
     */
    public boolean isUserOnline(String username) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, username);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            return false;
        }
    }
}

