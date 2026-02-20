package com.example.tomcat.bench.service;

import com.example.tomcat.bench.model.DbReadResponse;
import com.example.tomcat.bench.model.TxRequest;
import com.example.tomcat.bench.model.TxResponse;
import com.example.tomcat.bench.repository.BenchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
public class BenchService {

    private final BenchRepository benchRepository;
    private final TransactionalOperator transactionalOperator;

    public BenchService(BenchRepository benchRepository, TransactionalOperator transactionalOperator) {
        this.benchRepository = benchRepository;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<DbReadResponse> readWithSleep(long id, int sleepMs) {
        validateSleepMs(sleepMs);
        return benchRepository.sleep(sleepMs)
                .then(benchRepository.findById(id))
                .switchIfEmpty(Mono.error(new IllegalStateException("bench item not found. id=" + id)))
                .map(item -> new DbReadResponse(item.id(), item.payload(), item.cnt(), sleepMs));
    }

    public Mono<TxResponse> incrementInTransaction(TxRequest request, int sleepMs) {
        validateSleepMs(sleepMs);
        Mono<Void> sleepMono = sleepMs > 0 ? benchRepository.sleep(sleepMs) : Mono.empty();

        Mono<TxResponse> txFlow = sleepMono
                .then(benchRepository.incrementCount(request.id(), request.delta()))
                .flatMap(updatedRows -> {
                    if (updatedRows == 0) {
                        return Mono.error(new IllegalStateException("bench item not found. id=" + request.id()));
                    }
                    return benchRepository.findCountById(request.id())
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "bench item not found after update. id=" + request.id()
                            )))
                            .map(cnt -> new TxResponse(request.id(), cnt, request.delta(), sleepMs));
                });

        return transactionalOperator.transactional(txFlow);
    }

    private void validateSleepMs(int sleepMs) {
        if (sleepMs < 0) {
            throw new IllegalArgumentException("sleepMs must be >= 0");
        }
    }
}
