package it.polimi.gpplib.eforms_gpp_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class DummyController {
    @GetMapping("/unicorn")
    public String unicorn() {
        log.info("Unicorn endpoint was called");

        // Notice notice = new Notice(null);

        // GppNoticeAnalyzer analyzer = new DefaultGppNoticeAnalyzer();

        return "🦄 This is the unicorn endpoint!";
    }
}
