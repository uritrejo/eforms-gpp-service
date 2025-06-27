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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    @PostMapping(value = "/visualize-notice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VisualizeNoticeResponse> visualizeNotice(
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

        VisualizeNoticeResponse response = new VisualizeNoticeResponse();

        try {
            ResponseEntity<String> tedResponse = restTemplate.exchange(
                    tedApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            // Success case - TED API returned HTML
            response.setNoticeHtml(tedResponse.getBody());
            response.setSummary("Visualization completed successfully");
            response.setVisualizationStatus(tedResponse.getStatusCode().value());

            return ResponseEntity.ok(response);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle HTTP client errors (4xx status codes)
            TedApiErrorResponse errorResponse = parseErrorResponse(e.getResponseBodyAsString());

            response.setNoticeHtml(null);
            response.setSummary("Fatal error: " + errorResponse.getMessage());
            response.setVisualizationStatus(e.getStatusCode().value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Handle other exceptions (network issues, etc.)
            response.setNoticeHtml(null);
            response.setSummary("Fatal error: " + e.getMessage());
            response.setVisualizationStatus(500);

            return ResponseEntity.ok(response);
        }
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

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle HTTP client errors (4xx status codes)
            TedApiErrorResponse errorResponse = parseErrorResponse(e.getResponseBodyAsString());

            response.setValidationReportXml(null);
            response.setSummary("Fatal error: " + errorResponse.getMessage());
            response.setValidationStatus(e.getStatusCode().value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Handle other exceptions (network issues, etc.)
            response.setValidationReportXml(null);
            response.setSummary("Fatal error: " + e.getMessage());
            response.setValidationStatus(500);

            return ResponseEntity.ok(response);
        }
    }

    private TedApiErrorResponse parseErrorResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseBody, TedApiErrorResponse.class);
        } catch (Exception e) {
            log.warn("Could not parse error response from TED API: {}", responseBody, e);
            return new TedApiErrorResponse("Unknown error occurred");
        }
    }

    // Helper class for TED API error responses
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TedApiErrorResponse {
        private String message;

        public TedApiErrorResponse() {
            // Default constructor for Jackson
        }

        public TedApiErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
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

    public static class VisualizeNoticeResponse {
        private String noticeHtml;
        private String summary;
        private int visualizationStatus;

        public String getNoticeHtml() {
            return noticeHtml;
        }

        public void setNoticeHtml(String noticeHtml) {
            this.noticeHtml = noticeHtml;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public int getVisualizationStatus() {
            return visualizationStatus;
        }

        public void setVisualizationStatus(int visualizationStatus) {
            this.visualizationStatus = visualizationStatus;
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