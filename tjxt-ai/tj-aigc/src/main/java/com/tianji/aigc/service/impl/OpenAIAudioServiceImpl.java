package com.tianji.aigc.service.impl;

import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class OpenAIAudioServiceImpl implements AudioService {

    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        SpeechPrompt prompt = new SpeechPrompt(text); // 通过文本创建提示

        Flux<SpeechResponse> response = openAiAudioSpeechModel.stream(prompt); // 生成响应

        // 订阅响应，并将音频数据发送给客户端
        response.subscribe(speechResponse -> {
                    try {
                        byte[] audioBytes = speechResponse.getResult().getOutput();
                        emitter.send(audioBytes);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete);

        return emitter;
    }
}
