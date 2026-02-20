package com.example.tomcat.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BenchWebSocketHandler implements WebSocketHandler {

    private final WsMetrics wsMetrics;

    public BenchWebSocketHandler(WsMetrics wsMetrics) {
        this.wsMetrics = wsMetrics;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        wsMetrics.onOpen();

        Flux<WebSocketMessage> outbound = session.receive()
                .doOnNext(message -> wsMetrics.onMessage())
                .map(WebSocketMessage::getPayloadAsText)
                .map(payload -> session.textMessage("echo:" + payload));

        return session.send(outbound)
                .doFinally(signalType -> wsMetrics.onClose());
    }
}
