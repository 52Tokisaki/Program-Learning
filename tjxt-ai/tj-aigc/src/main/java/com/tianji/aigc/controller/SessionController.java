package com.tianji.aigc.controller;


import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {
    private final ChatSessionService chatSessionService;

    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer n) {
        log.info("create session with n: {}", n);
        return chatSessionService.createSession(n);
    }
}
