package pharma.agent.platform;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import pharma.dto.AgentRequestEnvelope;
import pharma.dto.AgentResponseEnvelope;

public class AgentGateway {
    private final Map<String, CompletableFuture<AgentResponseEnvelope<?>>> pendingRequests = new ConcurrentHashMap<>();

    public CompletableFuture<AgentResponseEnvelope<?>> submit(AgentRequestEnvelope<?> request) {
        CompletableFuture<AgentResponseEnvelope<?>> future = new CompletableFuture<>();
        pendingRequests.put(request.getTransactionId(), future);
        return future;
    }

    public void complete(AgentResponseEnvelope<?> response) {
        CompletableFuture<AgentResponseEnvelope<?>> future = pendingRequests.remove(response.getTransactionId());
        if (future != null) {
            future.complete(response);
        }
    }

    public int pendingRequestCount() {
        return pendingRequests.size();
    }
}
