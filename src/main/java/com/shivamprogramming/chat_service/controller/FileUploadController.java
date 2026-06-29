package com.shivamprogramming.chat_service.controller;

import com.shivamprogramming.chat_service.model.ChatMessage;
import com.shivamprogramming.chat_service.kafka.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat/files")
public class FileUploadController {

    @Value("${chat.upload.dir:uploads}")
    private String uploadDir;

    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaProducerService kafkaProducerService;

    public FileUploadController(SimpMessagingTemplate messagingTemplate,
                                KafkaProducerService kafkaProducerService) {
        this.messagingTemplate = messagingTemplate;
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderId") String senderId,
            @RequestParam("roomId") String roomId,
            @RequestParam(value = "recipientId", required = false) String recipientId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large. Max: 10MB"));
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID() + extension;

            Files.copy(file.getInputStream(), uploadPath.resolve(storedFilename),
                    StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/chat/files/" + storedFilename;

            ChatMessage chatMessage = ChatMessage.builder()
                    .senderId(senderId)
                    .recipientId(recipientId)
                    .content(originalFilename)
                    .timestamp(LocalDateTime.now())
                    .type(ChatMessage.MessageType.FILE)
                    .roomId(roomId)
                    .status(ChatMessage.DeliveryStatus.SENT)
                    .fileUrl(fileUrl)
                    .fileName(originalFilename)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .build();

            // Broadcast to appropriate channel
            if (recipientId != null && !recipientId.isEmpty()) {
                messagingTemplate.convertAndSendToUser(recipientId, "/queue/private", chatMessage);
            } else if ("public".equals(roomId)) {
                messagingTemplate.convertAndSend("/topic/public", chatMessage);
            } else {
                messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
            }

            kafkaProducerService.sendOrderEvent(chatMessage);
            log.info("File uploaded: {} by {} to room {}", originalFilename, senderId, roomId);

            return ResponseEntity.ok(Map.of(
                    "fileUrl", fileUrl,
                    "fileName", originalFilename != null ? originalFilename : "",
                    "fileType", file.getContentType() != null ? file.getContentType() : "",
                    "fileSize", file.getSize()
            ));

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] content = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .body(content);

        } catch (IOException e) {
            log.error("File read failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
