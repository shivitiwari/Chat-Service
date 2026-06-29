package com.shivamprogramming.chat_service.repository;

import com.shivamprogramming.chat_service.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // ── Fetch all messages in a room (public / private) ─────
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId);

    // ── Fetch last N messages in a room ─────────────────────
    List<ChatMessage> findTop50ByRoomIdOrderByTimestampDesc(String roomId);

    // ── Fetch conversation between two specific users ────────
    List<ChatMessage> findBySenderIdAndRecipientIdOrderByTimestampAsc(
            String senderId, String recipientId);

    // ── Fetch messages after a certain time (for pagination) ─
    List<ChatMessage> findByRoomIdAndTimestampAfterOrderByTimestampAsc(
            String roomId, LocalDateTime after);

    // ── Count undelivered messages for a recipient ───────────
    long countByRecipientIdAndDeliveredFalse(String recipientId);
}

