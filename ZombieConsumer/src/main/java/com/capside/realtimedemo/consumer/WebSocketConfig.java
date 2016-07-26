package com.capside.realtimedemo.consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 *
 * @author javi
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Utiliza este endpoint para connectar desde el navegador
        registry
            .setErrorHandler(new ErrorHandler())
            .addEndpoint("/stomp").withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(64 * 1024)
            .setSendBufferSizeLimit(1024*10 * 1024)
            .setSendTimeLimit(60 * 1000);
    }

    private class ErrorHandler extends StompSubProtocolErrorHandler {

        @Override
        public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
            return super.handleClientMessageProcessingError(clientMessage, ex);
        }

        @Override
        protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload, Throwable cause, StompHeaderAccessor clientHeaderAccessor) {
            return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
        }

        @Override
        public Message<byte[]> handleErrorMessageToClient(Message<byte[]> errorMessage) {
            return super.handleErrorMessageToClient(errorMessage);
        }

    }

}
