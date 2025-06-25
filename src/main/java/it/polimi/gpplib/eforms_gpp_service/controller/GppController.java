package it.polimi.gpplib.eforms_gpp_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import it.polimi.gpplib.eforms_gpp_service.config.AppConfig;

import java.util.List;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import java.util.Base64;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class GppController {

    private final GppNoticeAnalyzer analyzer = new DefaultGppNoticeAnalyzer();

    @Autowired
    private AppConfig appConfig;

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

    @PostMapping(value = "/visualize-notice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> visualizeNotice(
            @RequestBody VisualizeNoticeRequest request,
            @RequestParam(name = "language", required = false, defaultValue = "en") String language) {

        log.info("Received visualize-notice request");

        // Encode XML to Base64
        String base64Xml = Base64.getEncoder().encodeToString(request.getNoticeXml().getBytes());

        // Prepare request body for TED API
        String tedRequestBody = String.format(
                "{\"file\":\"%s\",\"language\":\"%s\",\"format\":\"HTML\",\"summary\":false}",
                base64Xml, language);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
        headers.setBearerAuth(appConfig.getApi().getTedApiKey());

        HttpEntity<String> requestEntity = new HttpEntity<>(tedRequestBody, headers);

        // Call TED API
        RestTemplate restTemplate = new RestTemplate();
        String tedApiUrl = "https://api.ted.europa.eu/v3/notices/render";
        ResponseEntity<String> tedResponse = restTemplate.exchange(
                tedApiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class);

        // Forward HTML response
        return ResponseEntity.status(tedResponse.getStatusCode())
                .contentType(MediaType.TEXT_HTML)
                .body(tedResponse.getBody());
    }

    @PostMapping(value = "/validate-notice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ValidateNoticeResponse> validateNotice(
            @RequestBody ValidateNoticeRequest request) {

        log.info("Received validate-notice request");

        // Load notice to get the eForms SDK version
        Notice notice = analyzer.loadNotice(request.getNoticeXml());
        String eFormsSdkVersion = notice.getEFormsSdkVersion();

        // Encode XML to Base64
        String base64Xml = Base64.getEncoder().encodeToString(request.getNoticeXml().getBytes());

        // Prepare request body for TED API
        String tedRequestBody = String.format(
                "{\"notice\":\"%s\",\"language\":\"%s\",\"validationMode\":\"%s\",\"eFormsSdkVersion\":\"%s\"}",
                base64Xml, request.getLanguage(), request.getValidationMode(), eFormsSdkVersion);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        headers.setBearerAuth(appConfig.getApi().getTedApiKey());

        HttpEntity<String> requestEntity = new HttpEntity<>(tedRequestBody, headers);

        // Call TED API
        RestTemplate restTemplate = new RestTemplate();
        String tedApiUrl = "https://api.ted.europa.eu/v3/notices/validate";

        ValidateNoticeResponse response = new ValidateNoticeResponse();

        try {
            ResponseEntity<String> tedResponse = restTemplate.exchange(
                    tedApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            // Success case - TED API returned validation report as XML
            response.setValidationReportXml(tedResponse.getBody());
            response.setSummary("Validation completed successfully");
            response.setValidationStatus(tedResponse.getStatusCode().value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Error case - extract error details
            String errorMessage = e.getMessage();
            int statusCode = 500; // default

            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpError = (org.springframework.web.client.HttpClientErrorException) e;
                statusCode = httpError.getStatusCode().value();

                // Try to extract the actual error message from the JSON response
                String responseBody = httpError.getResponseBodyAsString();
                try {
                    // Parse JSON to extract the message field
                    if (responseBody.contains("\"message\":")) {
                        int messageStart = responseBody.indexOf("\"message\":\"") + 11;
                        int messageEnd = responseBody.indexOf("\",", messageStart);
                        if (messageEnd == -1) {
                            messageEnd = responseBody.indexOf("\"}", messageStart);
                        }
                        if (messageStart > 10 && messageEnd > messageStart) {
                            errorMessage = responseBody.substring(messageStart, messageEnd)
                                    .replace("\\n", " ")
                                    .replace("\\\"", "\"");
                        }
                    }
                } catch (Exception parseException) {
                    log.warn("Could not parse error message from TED API response", parseException);
                }
            }

            response.setValidationReportXml(null);
            response.setSummary("Fatal error: " + errorMessage);
            response.setValidationStatus(statusCode);

            // Return 200 OK but with error details in the response body
            return ResponseEntity.ok(response);
        }
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

    public static class VisualizeNoticeRequest {
        private String noticeXml;

        public String getNoticeXml() {
            return noticeXml;
        }

        public void setNoticeXml(String noticeXml) {
            this.noticeXml = noticeXml;
        }
    }

    public static class ValidateNoticeRequest {
        private String noticeXml;
        private String language = "en";
        private String validationMode = "static";

        public String getNoticeXml() {
            return noticeXml;
        }

        public void setNoticeXml(String noticeXml) {
            this.noticeXml = noticeXml;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getValidationMode() {
            return validationMode;
        }

        public void setValidationMode(String validationMode) {
            this.validationMode = validationMode;
        }
    }

    public static class ValidateNoticeResponse {
        private String validationReportXml;
        private String summary;
        private int validationStatus;

        public String getValidationReportXml() {
            return validationReportXml;
        }

        public void setValidationReportXml(String validationReportXml) {
            this.validationReportXml = validationReportXml;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public int getValidationStatus() {
            return validationStatus;
        }

        public void setValidationStatus(int validationStatus) {
            this.validationStatus = validationStatus;
        }
    }
}