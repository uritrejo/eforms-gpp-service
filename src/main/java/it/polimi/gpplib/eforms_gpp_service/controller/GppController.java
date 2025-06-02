package it.polimi.gpplib.eforms_gpp_service.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import it.polimi.gpplib.DefaultGppNoticeAnalyzer;
import it.polimi.gpplib.GppNoticeAnalyzer;
import it.polimi.gpplib.model.Notice;
import it.polimi.gpplib.model.GppAnalysisResult;
import it.polimi.gpplib.model.SuggestedGppCriterion;
import it.polimi.gpplib.model.SuggestedGppPatch;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class GppController {

    private final GppNoticeAnalyzer analyzer = new DefaultGppNoticeAnalyzer();

    private Notice dummyNotice;

    @PostMapping(value = "/analyze-notice", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GppAnalysisResult> analyzeNotice(@RequestBody String xmlString) {
        log.info("Received analyze request");
        Notice notice = analyzer.loadNotice(xmlString);

        // ??++ tmp
        dummyNotice = notice;

        GppAnalysisResult result = analyzer.analyzeNotice(notice);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/suggest-patches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuggestedPatchesResponse> suggestPatches(@RequestBody SuggestedPatchesRequest request) {
        log.info("Received suggest-patches request");
        // Notice notice = analyzer.loadNotice(request.getNoticeXml());

        // ??++ tmp
        Notice notice = dummyNotice;

        List<SuggestedGppPatch> patches = analyzer.suggestPatches(notice, request.getCriteria());
        return ResponseEntity.ok(new SuggestedPatchesResponse(patches));
    }

    public static class SuggestedPatchesRequest {
        private String noticeXml;
        private List<SuggestedGppCriterion> criteria;

        public String getNoticeXml() {
            return noticeXml;
        }

        public void setNoticeXml(String noticeXml) {
            this.noticeXml = noticeXml;
        }

        public List<SuggestedGppCriterion> getCriteria() {
            return criteria;
        }

        public void setCriteria(List<SuggestedGppCriterion> criteria) {
            this.criteria = criteria;
        }
    }

    public static class SuggestedPatchesResponse {
        private List<SuggestedGppPatch> suggestedPatches;

        public SuggestedPatchesResponse(List<SuggestedGppPatch> patches) {
            this.suggestedPatches = patches;
        }

        public List<SuggestedGppPatch> getSuggestedPatches() {
            return suggestedPatches;
        }

        public void setSuggestedPatches(List<SuggestedGppPatch> patches) {
            this.suggestedPatches = patches;
        }
    }
}
