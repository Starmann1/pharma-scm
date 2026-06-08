package pharma.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class DomainEvent {
    private final String eventId;
    private final DomainEventType eventType;
    private final String entityType;
    private final String entityId;
    private final String details;
    private final LocalDateTime occurredAt;

    public DomainEvent(DomainEventType eventType, String entityType, String entityId, String details) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.occurredAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public DomainEventType getEventType() {
        return eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
