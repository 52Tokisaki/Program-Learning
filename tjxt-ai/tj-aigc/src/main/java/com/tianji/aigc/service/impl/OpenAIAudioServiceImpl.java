package com.tianji.aigc.service.impl;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class OpenAIAudioServiceImpl implements AudioService {

    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

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

    @Override
    public String stt(MultipartFile audioFile) {
        // 获取音频文件资源
        Resource fileResource = audioFile.getResource();
        // 创建音频转文本提示
        AudioTranscriptionPrompt audioTranscriptionPrompt = new AudioTranscriptionPrompt(fileResource);
        // 调用音频转文本模型, 获取响应
        AudioTranscriptionResponse audioTranscriptionResponse = openAiAudioTranscriptionModel.call(audioTranscriptionPrompt);
        String output = audioTranscriptionResponse.getResult().getOutput();
        return ZhConverterUtil.toSimple(output);
    }
}
