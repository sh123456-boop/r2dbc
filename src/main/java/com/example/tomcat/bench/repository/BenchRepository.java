package com.example.tomcat.bench.repository;

import com.example.tomcat.bench.model.BenchItem;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class BenchRepository {

    private final DatabaseClient databaseClient;

    public BenchRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<Void> sleep(int sleepMs) {
        double seconds = sleepMs / 1000.0;
        return databaseClient.sql("SELECT SLEEP(:seconds)")
                .bind("seconds", seconds)
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Mono<BenchItem> findById(long id) {
        return databaseClient.sql("SELECT id, payload, cnt FROM bench_items WHERE id = :id")
                .bind("id", id)
                .map((row, metadata) -> new BenchItem(
                        row.get("id", Long.class),
                        row.get("payload", String.class),
                        row.get("cnt", Long.class)
                ))
                .one();
    }

    public Mono<Long> incrementCount(long id, long delta) {
        return databaseClient.sql("UPDATE bench_items SET cnt = cnt + :delta WHERE id = :id")
                .bind("delta", delta)
                .bind("id", id)
                .fetch()
                .rowsUpdated();
    }

    public Mono<Long> findCountById(long id) {
        return databaseClient.sql("SELECT cnt FROM bench_items WHERE id = :id")
                .bind("id", id)
                .map((row, metadata) -> row.get("cnt", Long.class))
                .one();
    }
}
