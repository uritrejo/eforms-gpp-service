package it.polimi.gpplib.eforms_gpp_service.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import it.polimi.gpplib.DefaultGppNoticeAnalyzer;
import it.polimi.gpplib.GppNoticeAnalyzer;
import it.polimi.gpplib.model.Notice;
import it.polimi.gpplib.model.GppAnalysisResult;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class GppController {

    private final GppNoticeAnalyzer analyzer = new DefaultGppNoticeAnalyzer();

    @PostMapping(value = "/analyze-notice", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GppAnalysisResult> analyzeNotice(@RequestBody String xmlString) {
        log.info("Received analyze request");
        Notice notice = analyzer.loadNotice(xmlString);
        GppAnalysisResult result = analyzer.analyzeNotice(notice);
        return ResponseEntity.ok(result);
    }
}
