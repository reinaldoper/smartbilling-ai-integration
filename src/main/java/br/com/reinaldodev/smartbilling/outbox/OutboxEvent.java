package br.com.reinaldodev.smartbilling.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_status_created", columnList = "publishedAt,createdAt"))
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(nullable = false) private String aggregateType;
    @Column(nullable = false) private String aggregateId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(nullable = false) private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEvent() {}
    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void markPublished() { this.publishedAt = Instant.now(); }
}
