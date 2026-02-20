package com.example.tomcat.ws;

import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class WsChatRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS ws_chat_messages (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                room_id VARCHAR(64) NOT NULL,
                sender VARCHAR(64) NOT NULL,
                message TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_ws_chat_room_id_id (room_id, id DESC)
            )
            """;

    private final DatabaseClient databaseClient;

    public WsChatRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> ensureSchema() {
        return databaseClient.sql(CREATE_TABLE_SQL)
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Mono<Void> sleep(int sleepMs) {
        double seconds = sleepMs / 1000.0;
        return databaseClient.sql("SELECT SLEEP(:seconds)")
                .bind("seconds", seconds)
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Mono<Void> save(String roomId, String sender, String message) {
        return databaseClient.sql("INSERT INTO ws_chat_messages (room_id, sender, message) VALUES (:roomId, :sender, :message)")
                .bind("roomId", roomId)
                .bind("sender", sender)
                .bind("message", message)
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> {
                    if (rows == null || rows == 0) {
                        return Mono.error(new IllegalStateException("failed to insert message"));
                    }
                    return Mono.empty();
                })
                .then();
    }

    public Flux<WsChatMessage> findRecent(String roomId, int limit) {
        return databaseClient.sql("""
                        SELECT id, room_id, sender, message, created_at
                        FROM ws_chat_messages
                        WHERE room_id = :roomId
                        ORDER BY id DESC
                        LIMIT :limit
                        """)
                .bind("roomId", roomId)
                .bind("limit", limit)
                .map((row, metadata) -> new WsChatMessage(
                        row.get("id", Long.class),
                        row.get("room_id", String.class),
                        row.get("sender", String.class),
                        row.get("message", String.class),
                        row.get("created_at", LocalDateTime.class)
                ))
                .all();
    }

    
}
