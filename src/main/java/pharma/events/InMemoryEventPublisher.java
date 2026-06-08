package pharma.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {
    private final List<DomainEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> snapshot() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }
}
