package pharma.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentRequestEnvelope<T> {
    private String transactionId;
    private String action;
    private int requestedByUserId;
    private LocalDateTime createdAt;
    private long deadlineMillis;
    private T payload;

    public AgentRequestEnvelope() {
    }

    public AgentRequestEnvelope(String action, int requestedByUserId, long deadlineMillis, T payload) {
        this.transactionId = UUID.randomUUID().toString();
        this.action = action;
        this.requestedByUserId = requestedByUserId;
        this.createdAt = LocalDateTime.now();
        this.deadlineMillis = deadlineMillis;
        this.payload = payload;
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

    public int getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(int requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getDeadlineMillis() {
        return deadlineMillis;
    }

    public void setDeadlineMillis(long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
