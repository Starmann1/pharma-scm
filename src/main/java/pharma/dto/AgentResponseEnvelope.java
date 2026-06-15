package pharma.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pharma.agent.ontology.AgentStatuses;

@SuppressWarnings("null")
public class AgentResponseEnvelope<T> {
    private String transactionId;
    private String action;
    private AgentStatuses responseStatus;
    private LocalDateTime completedAt;
    private T payload;
    private List<String> errors = new ArrayList<>();
    private List<String> agentTrace = new ArrayList<>();

    public AgentResponseEnvelope() {
    }

    public AgentResponseEnvelope(String transactionId, String action, AgentStatuses responseStatus, T payload) {
        this.transactionId = transactionId;
        this.action = action;
        this.responseStatus = responseStatus;
        this.completedAt = LocalDateTime.now();
        this.payload = payload;
    }

    public static <T> AgentResponseEnvelope<T> success(String transactionId, String action, T payload) {
        return new AgentResponseEnvelope<>(transactionId, action, AgentStatuses.SUCCESS, payload);
    }

    public static <T> AgentResponseEnvelope<T> failure(String transactionId, String action, String error) {
        AgentResponseEnvelope<T> response = new AgentResponseEnvelope<>(transactionId, action, AgentStatuses.FAILURE, null);
        response.getErrors().add(error);
        return response;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public AgentStatuses getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(AgentStatuses responseStatus) {
        this.responseStatus = responseStatus;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getAgentTrace() {
        return agentTrace;
    }

    public void setAgentTrace(List<String> agentTrace) {
        this.agentTrace = agentTrace;
    }
}
