package com.example.websocket;

import java.util.logging.Logger;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WebSocketChatHandler extends TextWebSocketHandler {

    private final static Logger LOG = Logger.getGlobal();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String input = message.getPayload();
        LOG.info(input); // 채팅 log
        TextMessage textMessage = new TextMessage("웹소켓 테스트입니다.");
        session.sendMessage(textMessage);
    }
}