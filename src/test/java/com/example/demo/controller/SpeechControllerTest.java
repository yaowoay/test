package com.example.demo.controller;

import com.example.demo.service.AudioCaptureService;
import com.example.demo.service.SpeechRecognitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 语音控制器测试
 * 
 * @author example
 * @version 1.0.0
 */
@WebMvcTest(SpeechController.class)
class SpeechControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AudioCaptureService audioCaptureService;

    @MockBean
    private SpeechRecognitionService speechRecognitionService;

    @Test
    void startRecognition_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/speech/start"))
                .andExpect(status().isOk())
                .andExpect(content().string("Speech recognition started"));
    }

    @Test
    void stopRecognition_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/speech/stop"))
                .andExpect(status().isOk())
                .andExpect(content().string("Speech recognition stopped"));
    }
}
