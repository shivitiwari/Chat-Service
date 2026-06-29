package com.shivamprogramming.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndex(name = "sender_recipient_idx", def = "{'senderId': 1, 'recipientId': 1}")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private String senderId;

    @Indexed
    private String recipientId;

    private String content;

    @CreatedDate
    private LocalDateTime timestamp;

    private MessageType type;

    // ── Chat room / conversation ID ─────────────────────────
    @Indexed
    private String roomId;

    // ── Delivery Status ─────────────────────────────────────
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.SENT;

    // Legacy field — kept for backward compat
    @Builder.Default
    private boolean delivered = false;

    // ── File Attachment ──────────────────────────────────────
    private String fileUrl;          // URL to the uploaded file
    private String fileName;         // Original filename
    private String fileType;         // MIME type (image/png, application/pdf, etc.)
    private Long fileSize;           // Size in bytes

    public enum MessageType {
        CHAT, JOIN, LEAVE, FILE, TYPING, DELIVERY_UPDATE
    }

    public enum DeliveryStatus {
        SENT,       // Message sent by sender
        DELIVERED,  // Message delivered to recipient's device
        READ        // Message read/seen by recipient
    }
}

