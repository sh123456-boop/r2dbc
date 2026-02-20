package com.example.tomcat.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class WsChatWebSocketHandler implements WebSocketHandler {

    private final WsMetrics wsMetrics;
    private final WsChatService wsChatService;

    public WsChatWebSocketHandler(WsMetrics wsMetrics, WsChatService wsChatService) {
        this.wsMetrics = wsMetrics;
        this.wsChatService = wsChatService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        wsMetrics.onOpen();

        Flux<WebSocketMessage> outbound = session.receive()
                .doOnNext(message -> wsMetrics.onMessage())
                .map(WebSocketMessage::getPayloadAsText)
                .concatMap(wsChatService::handle)
                .map(session::textMessage);

        return session.send(outbound)
                .doFinally(signalType -> wsMetrics.onClose());
    }
}
