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

    // temporary storage for manual testing
    private Notice dummyNotice;

    @PostMapping(value = "/analyze-notice", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GppAnalysisResult> analyzeNotice(
            @RequestBody String xmlString,
            @RequestParam(name = "manualTesting", required = false, defaultValue = "false") boolean manualTesting) {

        log.info("Received analyze request");

        Notice notice = analyzer.loadNotice(xmlString);
        if (manualTesting) {
            log.info("Manual testing mode enabled for /analyze-notice");
            dummyNotice = notice;
        }

        GppAnalysisResult result = analyzer.analyzeNotice(notice);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/suggest-patches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuggestPatchesResponse> suggestPatches(
            @RequestBody SuggestPatchesRequest request,
            @RequestParam(name = "manualTesting", required = false, defaultValue = "false") boolean manualTesting) {

        log.info("Received suggest-patches request");

        Notice notice;
        if (manualTesting) {
            log.info("Manual testing mode enabled for /suggest-patches");
            notice = dummyNotice;
        } else {
            notice = analyzer.loadNotice(request.getNoticeXml());
        }

        List<SuggestedGppPatch> patches = analyzer.suggestPatches(notice, request.getCriteria());
        return ResponseEntity.ok(new SuggestPatchesResponse(patches));
    }

    @PostMapping(value = "/apply-patches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplyPatchesResponse> applyPatches(
            @RequestBody ApplyPatchesRequest request,
            @RequestParam(name = "manualTesting", required = false, defaultValue = "false") boolean manualTesting) {

        log.info("Received apply-patches request");

        Notice notice;
        if (manualTesting) {
            log.info("Manual testing mode enabled for /apply-patches");
            notice = dummyNotice;
        } else {
            notice = analyzer.loadNotice(request.getNoticeXml());
        }

        Notice patchedNotice = analyzer.applyPatches(notice, request.getPatches());
        String patchedNoticeXml = patchedNotice.toXmlString();

        log.info("Patched notice XML");

        return ResponseEntity.ok(new ApplyPatchesResponse(patchedNoticeXml));
    }

    public static class SuggestPatchesRequest {
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

    public static class SuggestPatchesResponse {
        private List<SuggestedGppPatch> suggestedPatches;

        public SuggestPatchesResponse(List<SuggestedGppPatch> patches) {
            this.suggestedPatches = patches;
        }

        public List<SuggestedGppPatch> getSuggestedPatches() {
            return suggestedPatches;
        }

        public void setSuggestedPatches(List<SuggestedGppPatch> patches) {
            this.suggestedPatches = patches;
        }
    }

    public static class ApplyPatchesRequest {
        private String noticeXml;
        private List<SuggestedGppPatch> patches;

        public String getNoticeXml() {
            return noticeXml;
        }

        public void setNoticeXml(String noticeXml) {
            this.noticeXml = noticeXml;
        }

        public List<SuggestedGppPatch> getPatches() {
            return patches;
        }

        public void setPatches(List<SuggestedGppPatch> patches) {
            this.patches = patches;
        }
    }

    public static class ApplyPatchesResponse {
        private String patchedNoticeXml;

        public ApplyPatchesResponse(String patchedNoticeXml) {
            this.patchedNoticeXml = patchedNoticeXml;
        }

        public String getPatchedNoticeXml() {
            return patchedNoticeXml;
        }

        public void setPatchedNoticeXml(String patchedNoticeXml) {
            this.patchedNoticeXml = patchedNoticeXml;
        }
    }
}
