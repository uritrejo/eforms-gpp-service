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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "eForms GPP Controller", description = "API for Green Public Procurement analysis of eForms notices")
public class GppController {

    private final GppNoticeAnalyzer analyzer = new DefaultGppNoticeAnalyzer();

    @Autowired
    private AppConfig appConfig;

    // temporary storage for manual testing
    private Notice dummyNotice;

    @Operation(summary = "Analyze a procurement notice for GPP criteria", description = "Analyzes an eForms XML notice to identify Green Public Procurement (GPP) criteria and potential improvements")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GppAnalysisResult.class))),
            @ApiResponse(responseCode = "400", description = "Invalid XML notice provided"),
            @ApiResponse(responseCode = "500", description = "Internal server error during analysis")
    })
    @PostMapping(value = "/analyze-notice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GppAnalysisResult> analyzeNotice(
            @Parameter(description = "Request containing the eForms XML notice to analyze", required = true) @RequestBody AnalyzeNoticeRequest request,
            @Parameter(description = "Enable manual testing mode to store notice for subsequent requests") @RequestParam(name = "manualTesting", required = false, defaultValue = "false") boolean manualTesting) {

        log.info("Received analyze request");

        Notice notice = analyzer.loadNotice(request.getNoticeXml());
        if (manualTesting) {
            log.info("Manual testing mode enabled for /analyze-notice");
            dummyNotice = notice;
        }

        GppAnalysisResult result = analyzer.analyzeNotice(notice);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Suggest GPP patches for a notice", description = "Based on provided GPP criteria, suggests specific patches that can be applied to improve the procurement notice's GPP compliance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patches suggested successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuggestPatchesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error during patch suggestion")
    })
    @PostMapping(value = "/suggest-patches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuggestPatchesResponse> suggestPatches(
            @Parameter(description = "Request containing notice XML and GPP criteria", required = true) @RequestBody SuggestPatchesRequest request,
            @Parameter(description = "Use previously analyzed notice from manual testing mode") @RequestParam(name = "manualTesting", required = false, defaultValue = "false") boolean manualTesting) {

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

    @Operation(summary = "Apply GPP patches to a notice", description = "Applies the specified GPP patches to a procurement notice and returns the modified XML")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patches applied successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApplyPatchesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid patches or notice XML"),
            @ApiResponse(responseCode = "500", description = "Internal server error during patch application")
    })
    @PostMapping(value = "/apply-patches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplyPatchesResponse> applyPatches(
            @Parameter(description = "Request containing notice XML and patches to apply", required = true) @RequestBody ApplyPatchesRequest request,
            @Parameter(description = "Use previously analyzed notice from manual testing mode") @RequestParam(name = "manualTesting", required = false, defaultValue = "false") boolean manualTesting) {

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

    @Operation(summary = "Visualize a procurement notice", description = "Converts an eForms XML notice to HTML visualization using the TED API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notice visualization completed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VisualizeNoticeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid notice XML"),
            @ApiResponse(responseCode = "401", description = "Invalid TED API key"),
            @ApiResponse(responseCode = "500", description = "TED API error or internal server error")
    })
    @PostMapping(value = "/visualize-notice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VisualizeNoticeResponse> visualizeNotice(
            @Parameter(description = "Request containing the eForms XML notice to visualize", required = true) @RequestBody VisualizeNoticeRequest request,
            @Parameter(description = "Language code for visualization (default: en)") @RequestParam(name = "language", required = false, defaultValue = "en") String language) {

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

    @Operation(summary = "Validate a procurement notice", description = "Validates an eForms XML notice using the TED API validation service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notice validation completed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidateNoticeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid notice XML or validation parameters"),
            @ApiResponse(responseCode = "401", description = "Invalid TED API key"),
            @ApiResponse(responseCode = "500", description = "TED API error or internal server error")
    })
    @PostMapping(value = "/validate-notice", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ValidateNoticeResponse> validateNotice(
            @Parameter(description = "Request containing the eForms XML notice to validate", required = true) @RequestBody ValidateNoticeRequest request) {

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

    @Schema(description = "Request to analyze an eForms notice for GPP criteria")
    public static class AnalyzeNoticeRequest {
        @Schema(description = "The eForms XML notice content to analyze", example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...")
        private String noticeXml;

        public String getNoticeXml() {
            return noticeXml;
        }

        public void setNoticeXml(String noticeXml) {
            this.noticeXml = noticeXml;
        }
    }

    @Schema(description = "Request to suggest GPP patches based on criteria")
    public static class SuggestPatchesRequest {
        @Schema(description = "The eForms XML notice content", example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...")
        private String noticeXml;
        @Schema(description = "List of GPP criteria to consider for patch suggestions")
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

    @Schema(description = "Response containing suggested GPP patches")
    public static class SuggestPatchesResponse {
        @Schema(description = "List of suggested patches to improve GPP compliance")
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

    @Schema(description = "Request to apply GPP patches to a notice")
    public static class ApplyPatchesRequest {
        @Schema(description = "The eForms XML notice content", example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...")
        private String noticeXml;
        @Schema(description = "List of patches to apply to the notice")
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

    @Schema(description = "Response containing the patched notice XML")
    public static class ApplyPatchesResponse {
        @Schema(description = "The modified eForms XML notice with patches applied")
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

    @Schema(description = "Request to visualize a notice as HTML")
    public static class VisualizeNoticeRequest {
        @Schema(description = "The eForms XML notice content to visualize", example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...")
        private String noticeXml;

        public String getNoticeXml() {
            return noticeXml;
        }

        public void setNoticeXml(String noticeXml) {
            this.noticeXml = noticeXml;
        }
    }

    @Schema(description = "Response containing the HTML visualization of the notice")
    public static class VisualizeNoticeResponse {
        @Schema(description = "HTML representation of the notice (null if visualization failed)")
        private String noticeHtml;
        @Schema(description = "Summary of the visualization process or error message")
        private String summary;
        @Schema(description = "HTTP status code from the TED API visualization service")
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

    @Schema(description = "Request to validate a notice using TED API")
    public static class ValidateNoticeRequest {
        @Schema(description = "The eForms XML notice content to validate", example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...")
        private String noticeXml;
        @Schema(description = "Language code for validation (default: en)", example = "en")
        private String language = "en";
        @Schema(description = "Validation mode (default: static)", example = "static", allowableValues = { "static",
                "dynamic" })
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

    @Schema(description = "Response containing the validation results")
    public static class ValidateNoticeResponse {
        @Schema(description = "XML validation report from TED API (null if validation failed)")
        private String validationReportXml;
        @Schema(description = "Summary of the validation process or error message")
        private String summary;
        @Schema(description = "HTTP status code from the TED API validation service")
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