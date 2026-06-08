package pharma.dto;

import java.util.ArrayList;
import java.util.List;

public class RootCauseDTO {
    private String incidentId;
    private String batchNumber;
    private String summary;
    private List<String> probableCauses = new ArrayList<>();
    private List<String> evidenceReferences = new ArrayList<>();

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getProbableCauses() {
        return probableCauses;
    }

    public void setProbableCauses(List<String> probableCauses) {
        this.probableCauses = probableCauses;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }
}
