package pharma.dto;

import java.util.ArrayList;
import java.util.List;

public class AIReasoningResultDTO {
    private Object extractedData;
    private double confidenceScore;
    private String modelUsed;
    private String promptSummary;
    private boolean requiresHumanReview;
    private List<CitationDTO> citations = new ArrayList<>();

    public AIReasoningResultDTO() {
    }

    public AIReasoningResultDTO(Object extractedData, double confidenceScore, String modelUsed,
            String promptSummary, boolean requiresHumanReview, List<CitationDTO> citations) {
        this.extractedData = extractedData;
        this.confidenceScore = confidenceScore;
        this.modelUsed = modelUsed;
        this.promptSummary = promptSummary;
        this.requiresHumanReview = requiresHumanReview;
        this.citations = citations;
    }

    public Object getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(Object extractedData) {
        this.extractedData = extractedData;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getPromptSummary() {
        return promptSummary;
    }

    public void setPromptSummary(String promptSummary) {
        this.promptSummary = promptSummary;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public void setRequiresHumanReview(boolean requiresHumanReview) {
        this.requiresHumanReview = requiresHumanReview;
    }

    public List<CitationDTO> getCitations() {
        return citations;
    }

    public void setCitations(List<CitationDTO> citations) {
        this.citations = citations;
    }
}
