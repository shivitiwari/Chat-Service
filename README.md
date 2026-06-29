# 🗨️ Chat Service — Real-Time Messaging Microservice

A production-grade real-time chat application built with **Spring Boot 3.2**, **WebSocket (STOMP/SockJS)**, **Apache Kafka**, **MongoDB**, **Redis**, and **JWT Authentication**.

---

## 📋 Tech Stack

| Technology | Purpose |
|------------|---------|
| **Spring Boot 3.2.5** | Application framework |
| **WebSocket + STOMP + SockJS** | Real-time bidirectional messaging |
| **Apache Kafka** | Event-driven message persistence pipeline |
| **MongoDB** | Chat message & room storage |
| **Redis** | Online users tracking + room membership caching |
| **Spring Security + JWT** | Authentication & authorization |
| **Lombok** | Boilerplate reduction |
| **Java 21** | Runtime |

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔐 **JWT Authentication** | Secure REST & WebSocket with JWT tokens (compatible with external Auth service) |
| 💬 **Public Chat Room** | Global broadcast room for all connected users |
| 🏠 **Named Chat Rooms** | Create/join/leave custom rooms with member tracking |
| 🔒 **Private DM** | Direct 1-on-1 messaging between users |
| 📎 **File Sharing** | Upload & share images/files in chat (max 10MB) |
| 📬 **Delivery Status** | Message status tracking: SENT → DELIVERED → READ |
| 🟢 **Online Presence** | Real-time online/offline user status via Redis |
| 📜 **Chat History** | Persistent message history via Kafka → MongoDB pipeline |
| 🎨 **Modern UI** | Beautiful dark-themed HTML/JS client with responsive design |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        BROWSER (SockJS Client)                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─── WebSocket (STOMP) ───┐         ┌─── REST API ───┐             │
│  │ CONNECT /ws             │         │ GET /api/chat/* │             │
│  │ SEND /app/chat.*        │         │ POST /api/chat/*│             │
│  │ SUBSCRIBE /topic/*      │         └────────┬────────┘             │
│  └────────────┬────────────┘                  │                       │
│               │                               │                       │
├───────────────┼───────────────────────────────┼───────────────────────┤
│               ▼                               ▼                       │
│  ┌─── Spring Security (JWT Filter) ──────────────────┐               │
│  │  JwtAuthenticationFilter (REST)                    │               │
│  │  WebSocketAuthInterceptor (STOMP CONNECT)          │               │
│  └────────────────────────┬───────────────────────────┘               │
│                           ▼                                           │
│  ┌─── ChatController ────────────────────────────────┐               │
│  │  /app/chat.sendMessage → /topic/public            │               │
│  │  /app/chat.addUser → /topic/public + Redis        │               │
│  │  /app/chat.room.join.{room} → /topic/room/{room}  │               │
│  │  /app/chat.room.send.{room} → /topic/room/{room}  │               │
│  │  /app/chat.privateMessage → /user/queue/private    │               │
│  └────────────────────────┬───────────────────────────┘               │
│                           │                                           │
│                  ┌────────┼────────┐                                  │
│                  ▼        ▼        ▼                                  │
│           ┌──────────┐ ┌──────┐ ┌───────────────────┐                │
│           │  Redis   │ │Kafka │ │SimpMessagingTemplate│               │
│           │(presence)│ │Topic │ │  (broadcast)        │               │
│           └──────────┘ └──┬───┘ └───────────────────┘                │
│                           │                                           │
│                           ▼                                           │
│              ┌─── KafkaConsumerService ───┐                           │
│              │  Consume → Save to MongoDB │                           │
│              └────────────┬───────────────┘                           │
│                           ▼                                           │
│                    ┌──────────────┐                                   │
│                    │   MongoDB    │                                   │
│                    │ (messages +  │                                   │
│                    │  chat_rooms) │                                   │
│                    └──────────────┘                                   │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Prerequisites

| Service | Port | Required |
|---------|------|----------|
| Java 21 | — | ✅ |
| MongoDB | 27017 | ✅ |
| Redis | 6379 | ✅ (for online users & rooms) |
| Apache Kafka | 9092 | ⚠️ Optional (set `auto-startup=false`) |

---

## ⚡ Quick Start

### 1. Start MongoDB
Ensure MongoDB is running on `localhost:27017`.

### 2. Start Redis
```bash
redis-server
```

### 3. Start Kafka (Optional — for message persistence)
```bash
# Using the provided script
start-kafka.bat

# Or manually (KRaft mode — no ZooKeeper)
cd C:\kafka\kafka_2.13-4.2.0
bin\windows\kafka-storage.bat random-uuid
bin\windows\kafka-storage.bat format -t <CLUSTER_ID> -c config\server.properties
bin\windows\kafka-server-start.bat config\server.properties
```

### 4. Start the Application
```bash
.\mvnw.cmd spring-boot:run
```

### 5. Open the Chat Client
```
http://localhost:8080
```

---

## 🔐 Authentication (JWT)

The service supports **JWT-based authentication** compatible with an external Auth/User service.

### How it works:

1. **REST API**: The `JwtAuthenticationFilter` checks `Authorization: Bearer <token>` header
2. **WebSocket**: The `WebSocketAuthInterceptor` validates JWT from the STOMP CONNECT frame header
3. **Anonymous mode**: If no token is provided, requests to permitted endpoints still work (for development)

### Configuration:
```properties
# application.properties — must match your Auth service secret
jwt.secret=dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9yand0YXV0aGVudGljYXRpb24xMjM0NTY3ODk=
```

### Connecting with JWT (WebSocket):
```javascript
stompClient.connect(
    { Authorization: "Bearer <your-jwt-token>" },
    onConnected,
    onError
);
```

### Calling REST API with JWT:
```
GET http://localhost:8080/api/chat/history/public
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 📡 REST API Endpoints

### Base URL: `http://localhost:8080`

---

### 📜 Chat History

#### 1. Get Full Chat History

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/chat/history/{roomId}` |
| **Headers** | `Authorization: Bearer <token>` (optional) |

```
GET http://localhost:8080/api/chat/history/public
```

**Response (200):**
```json
[
  {
    "id": "6843a1b2c3d4e5f6a7b8c9d0",
    "senderId": "alice",
    "recipientId": "",
    "content": "Hello everyone!",
    "timestamp": "2026-06-07T10:30:00",
    "type": "CHAT",
    "roomId": "public",
    "status": "DELIVERED",
    "delivered": true,
    "fileUrl": null,
    "fileName": null,
    "fileType": null,
    "fileSize": null
  }
]
```

---

#### 2. Get Recent Messages (Last 50)

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/chat/history/{roomId}/recent` |

```
GET http://localhost:8080/api/chat/history/public/recent
```

---

#### 3. Get Unread Count

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/chat/unread/{recipientId}` |

```
GET http://localhost:8080/api/chat/unread/alice
```

**Response:** `5`

---

### 🟢 Online Users

#### 4. Get Online Users

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/chat/online-users` |

```
GET http://localhost:8080/api/chat/online-users
```

**Response:**
```json
["alice", "bob", "charlie"]
```

---

#### 5. Get Online User Count

```
GET http://localhost:8080/api/chat/online-users/count
```

**Response:**
```json
{ "count": 3 }
```

---

#### 6. Check User Status

```
GET http://localhost:8080/api/chat/online-users/alice/status
```

**Response:**
```json
{ "username": "alice", "online": true }
```

---

### 🏠 Chat Rooms

#### 7. List All Rooms

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/chat/rooms` |

```
GET http://localhost:8080/api/chat/rooms
```

**Response:**
```json
[
  {
    "id": "684f1a2b3c4d5e6f7a8b9c0d",
    "name": "general",
    "description": "General discussion",
    "createdBy": "alice",
    "createdAt": "2026-06-07T10:00:00"
  }
]
```

---

#### 8. Create a Room

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/chat/rooms` |
| **Content-Type** | `application/json` |

**Request Body:**
```json
{
  "name": "general",
  "description": "General discussion",
  "createdBy": "alice"
}
```

**Response (200):**
```json
{
  "id": "684f1a2b3c4d5e6f7a8b9c0d",
  "name": "general",
  "description": "General discussion",
  "createdBy": "alice",
  "createdAt": "2026-06-07T10:00:00"
}
```

---

#### 9. Get Room Members

```
GET http://localhost:8080/api/chat/rooms/general/members
```

**Response:**
```json
["alice", "bob"]
```

---

#### 10. Get User's Rooms

```
GET http://localhost:8080/api/chat/rooms/user/alice
```

**Response:**
```json
["public", "general", "dev-team"]
```

---

### 📎 File Upload

#### 11. Upload a File

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/chat/files/upload` |
| **Content-Type** | `multipart/form-data` |
| **Max Size** | 10 MB |

**Form Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | ✅ | The file to upload |
| `senderId` | String | ✅ | Who is sending |
| `roomId` | String | ✅ | Target room (e.g. `public`, `general`) |
| `recipientId` | String | ❌ | For private DM only |

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/chat/files/upload \
  -F "file=@/path/to/image.png" \
  -F "senderId=alice" \
  -F "roomId=public"
```

**Postman:**
- Method: POST
- URL: `http://localhost:8080/api/chat/files/upload`
- Body → form-data:
  - `file`: select file
  - `senderId`: `alice`
  - `roomId`: `public`

**Response (200):**
```json
{
  "fileUrl": "/api/chat/files/a1b2c3d4-image.png",
  "fileName": "image.png",
  "fileType": "image/png",
  "fileSize": 245678
}
```

---

#### 12. Download/View a File

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/chat/files/{filename}` |

```
GET http://localhost:8080/api/chat/files/a1b2c3d4-image.png
```

Returns the file with appropriate `Content-Type` header.

---

### 🏥 Health Check

#### 13. Actuator Health

```
GET http://localhost:8080/actuator/health
```

**Response:**
```json
{ "status": "UP" }
```

---

## 🔗 WebSocket API (STOMP over SockJS)

### Connection

| Field | Value |
|-------|-------|
| **Endpoint** | `http://localhost:8080/ws` (SockJS) |
| **Raw WebSocket** | `ws://localhost:8080/ws/websocket` |
| **Protocol** | STOMP 1.2 |

---

### STOMP Destinations

| Action | Send To | Broadcasts To |
|--------|---------|---------------|
| Send public message | `/app/chat.sendMessage` | `/topic/public` |
| Join public room | `/app/chat.addUser` | `/topic/public` + `/topic/online-users` |
| Join named room | `/app/chat.room.join.{roomName}` | `/topic/room/{roomName}` + `/topic/room/{roomName}/members` |
| Leave named room | `/app/chat.room.leave.{roomName}` | `/topic/room/{roomName}` + `/topic/room/{roomName}/members` |
| Send room message | `/app/chat.room.send.{roomName}` | `/topic/room/{roomName}` |
| Send private DM | `/app/chat.privateMessage` | `/user/{recipientId}/queue/private` |

---

### Subscribe Topics

| Topic | Description |
|-------|-------------|
| `/topic/public` | All public room messages |
| `/topic/online-users` | Live online users list (Set) |
| `/topic/room/{roomName}` | Messages for a specific room |
| `/topic/room/{roomName}/members` | Members list for a room |
| `/user/queue/private` | Private DMs for the current user |

---

### Testing WebSocket in Postman

> **Postman → New → WebSocket Request**

Connect to: `ws://localhost:8080/ws/websocket`

#### STOMP CONNECT:
```
CONNECT
accept-version:1.2
heart-beat:10000,10000
Authorization:Bearer <your-jwt-token>

\0
```

#### Subscribe to Public:
```
SUBSCRIBE
id:sub-0
destination:/topic/public

\0
```

#### Send Public Message:
```
SEND
destination:/app/chat.sendMessage
content-type:application/json

{"senderId":"alice","recipientId":"","content":"Hello World!","type":"CHAT"}\0
```

#### Join a Named Room:
```
SEND
destination:/app/chat.room.join.general
content-type:application/json

{"senderId":"alice","content":"","type":"JOIN"}\0
```

#### Send Message to Room:
```
SEND
destination:/app/chat.room.send.general
content-type:application/json

{"senderId":"alice","content":"Hello room!","type":"CHAT"}\0
```

#### Send Private DM:
```
SEND
destination:/app/chat.privateMessage
content-type:application/json

{"senderId":"alice","recipientId":"bob","content":"Hey Bob!","type":"CHAT"}\0
```

---

## 📦 Message JSON Schema

### ChatMessage (WebSocket & MongoDB)

```json
{
  "id": "string (MongoDB auto-generated)",
  "senderId": "string (required)",
  "recipientId": "string (empty for public, userId for DM)",
  "content": "string (message text or filename for FILE type)",
  "timestamp": "ISO DateTime (auto-set by server)",
  "type": "CHAT | JOIN | LEAVE | FILE | TYPING | DELIVERY_UPDATE",
  "roomId": "string ('public', room name, or sorted user pair)",
  "status": "SENT | DELIVERED | READ",
  "delivered": "boolean (legacy)",
  "fileUrl": "string (URL path for uploaded files)",
  "fileName": "string (original filename)",
  "fileType": "string (MIME type: image/png, etc.)",
  "fileSize": "number (file size in bytes)"
}
```

### Delivery Status Flow:

```
SENT ──────► DELIVERED ──────► READ
 │              │                │
 │ Sender      │ Recipient's   │ Recipient
 │ sends msg   │ client acks   │ opens chat
```

---

## 📂 Project Structure

```
src/main/java/com/shivamprogramming/chat_service/
├── ChatServiceApplication.java              # Main entry point
├── config/
│   ├── KafkaConsumerConfig.java             # Kafka consumer factory
│   ├── KafkaProducerConfig.java             # Kafka producer factory
│   ├── SecurityConfig.java                  # Spring Security + JWT filter chain
│   └── WebSocketConfig.java                 # STOMP/SockJS + auth interceptor
├── controller/
│   ├── ChatController.java                  # WebSocket message handlers
│   ├── ChatHistoryController.java           # REST API for history, rooms, users
│   └── FileUploadController.java            # File upload/download endpoints
├── dto/
│   └── ChatMessageEvent.java               # Kafka event DTO
├── kafka/
│   ├── KafkaConsumerService.java            # Kafka → MongoDB persistence
│   └── KafkaProducerService.java            # Publish messages to Kafka
├── listener/
│   └── WebSocketEventListener.java          # Connect/Disconnect event handling
├── model/
│   ├── ChatMessage.java                     # Message document (MongoDB)
│   └── ChatRoom.java                        # Room document (MongoDB)
├── repository/
│   ├── ChatMessageRepository.java           # Message queries
│   └── ChatRoomRepository.java              # Room queries
├── security/
│   ├── JwtService.java                      # JWT token validation
│   ├── JwtAuthenticationFilter.java         # HTTP request JWT filter
│   └── WebSocketAuthInterceptor.java        # STOMP CONNECT JWT validation
└── service/
    ├── ChatRoomService.java                 # Room CRUD + Redis membership
    └── OnlineUserService.java               # Online users via Redis Set

src/main/resources/
├── application.properties                   # All configuration
└── static/
    └── index.html                           # Chat UI (HTML/JS/CSS)
```

---

## ⚙️ Configuration

### `application.properties`

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | `8080` | Application port |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/chat_db` | MongoDB connection |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker |
| `spring.kafka.consumer.group-id` | `chat-service-group` | Consumer group |
| `kafka.topic.message-events` | `message-events` | Kafka topic |
| `spring.kafka.listener.auto-startup` | `false` | Set `true` when Kafka is running |
| `jwt.secret` | Base64 encoded key | Must match Auth service |
| `chat.upload.dir` | `uploads` | File upload directory |
| `spring.servlet.multipart.max-file-size` | `10MB` | Max upload size |
| `eureka.client.enabled` | `false` | Disable service discovery |

---

## 🧪 Testing the Full Flow

### Step 1: Start Infrastructure
```
MongoDB  → localhost:27017  (required)
Redis    → localhost:6379   (required)
Kafka    → localhost:9092   (optional — run start-kafka.bat)
```

### Step 2: Start the Application
```bash
.\mvnw.cmd spring-boot:run
```

### Step 3: Open Chat UI
```
http://localhost:8080
```

### Step 4: Test the flow
1. Enter username → Click "Connect & Chat"
2. Send public messages
3. Join/create rooms from the left sidebar
4. Click on online users to send DMs
5. Upload files using the file upload API

### Step 5: Verify Persistence (if Kafka is running)
```bash
# REST API
curl http://localhost:8080/api/chat/history/public

# MongoDB shell
mongosh
use chat_db
db.messages.find().sort({timestamp: -1}).limit(10).pretty()
db.chat_rooms.find().pretty()
```

### Step 6: Verify Redis
```bash
redis-cli
SMEMBERS chat:online-users
SMEMBERS chat:room:members:public
SMEMBERS chat:user:rooms:alice
```

---

## 🛑 Troubleshooting

| Issue | Solution |
|-------|----------|
| `Broker may not be available` | Start Kafka or set `spring.kafka.listener.auto-startup=false` |
| `Injection of autowired dependencies failed` (JwtService) | Ensure `jwt.secret` is set in `application.properties` |
| `DataSource url not specified` | Remove `spring-boot-starter-data-jpa` from pom.xml |
| Login popup on browser | SecurityConfig disables httpBasic/formLogin — clear cache |
| `allowedOrigins cannot contain "*"` | Use `setAllowedOriginPatterns("*")` (already fixed) |
| Empty `FileUploadController.java` | Ensure file has content (recreate if empty) |
| Redis connection refused | Start Redis server on port 6379 |
| Messages not persisting | Kafka must be running + `auto-startup=true` |
| WebSocket 403 Forbidden | Check CORS config and SecurityConfig permits `/ws/**` |

---

## 🔮 Future Enhancements

- [ ] Typing indicators (real-time "user is typing...")
- [ ] Message reactions (emoji reactions)
- [ ] Read receipts (bulk mark as read)
- [ ] Push notifications
- [ ] Message search (full-text search in MongoDB)
- [ ] Rate limiting
- [ ] Message encryption (end-to-end)
- [ ] User profiles & avatars

---

## 📄 License

This project is for educational/learning purposes.
