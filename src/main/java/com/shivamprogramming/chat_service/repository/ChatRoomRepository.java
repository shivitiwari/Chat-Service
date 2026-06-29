package com.shivamprogramming.chat_service.repository;

import com.shivamprogramming.chat_service.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findByName(String name);

    boolean existsByName(String name);
}