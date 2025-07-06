package com.example.demo.config;

import com.example.demo.handler.SpeechWebSocketHandler;
import com.example.demo.service.AudioCaptureService;
import com.example.demo.service.SpeechRecognitionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket配置类
 *
 * @author example
 * @version 1.0.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AudioCaptureService audioCaptureService;
    private final SpeechRecognitionService speechRecognitionService;

    public WebSocketConfig(AudioCaptureService audioCaptureService,
                          SpeechRecognitionService speechRecognitionService) {
        this.audioCaptureService = audioCaptureService;
        this.speechRecognitionService = speechRecognitionService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(speechWebSocketHandler(), "/speech")
                .setAllowedOrigins("*");
    }



    @Bean
    public WebSocketHandler speechWebSocketHandler() {
        return new SpeechWebSocketHandler(audioCaptureService, speechRecognitionService);
    }
}