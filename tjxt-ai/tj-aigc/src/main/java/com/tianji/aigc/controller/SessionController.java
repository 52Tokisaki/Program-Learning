package com.tianji.aigc.controller;


import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatSessionVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/hot")
    public List<SessionVO.Example> getHotSession(@RequestParam(value = "n", defaultValue = "3") Integer n) {
        return chatSessionService.getHotSession(n);
    }

    @GetMapping("/{sessionId}")
    public List<MessageVO> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return chatSessionService.queryBySessionId(sessionId);
    }

    @GetMapping("/history")
    public Map<String, List<ChatSessionVO>> queryHistorySession() {
        return chatSessionService.queryHistorySession();
    }

    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        chatSessionService.deleteHistorySession(sessionId);
    }

    @PutMapping("/history")
    public void updateTitle(@RequestParam("sessionId") String sessionId, @RequestParam("title") String title) {
        chatSessionService.updateTitle(sessionId, title);
    }
}
