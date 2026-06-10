package pharma.dto;

public class AIReasoningRequestDTO {
    private String taskType;
    private String rawText;
    private String contextSummary;

    public AIReasoningRequestDTO() {
    }

    public AIReasoningRequestDTO(String taskType, String rawText, String contextSummary) {
        this.taskType = taskType;
        this.rawText = rawText;
        this.contextSummary = contextSummary;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }
}
