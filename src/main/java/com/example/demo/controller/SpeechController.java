package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 语音识别控制器
 *
 * @author example
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    /**
     * 开始语音识别
     */
    @PostMapping("/start")
    public ResponseEntity<String> startRecognition() {
        log.info("Starting speech recognition");
        return ResponseEntity.ok("Speech recognition started");
    }

    /**
     * 停止语音识别
     */
    @PostMapping("/stop")
    public ResponseEntity<String> stopRecognition() {
        log.info("Stopping speech recognition");
        return ResponseEntity.ok("Speech recognition stopped");
    }
}
