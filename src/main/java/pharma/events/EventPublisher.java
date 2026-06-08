package pharma.events;

public interface EventPublisher {
    void publish(DomainEvent event);
}
