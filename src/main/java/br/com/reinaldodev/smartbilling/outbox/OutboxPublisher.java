package br.com.reinaldodev.smartbilling.outbox;

import br.com.reinaldodev.smartbilling.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demonstra o relay do Outbox. Em produção, use lock/claim de lote,
 * publisher confirms, retry com backoff e consumidor idempotente.
 */
@Component
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository; this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:3000}")
    @Transactional
    public void publishPending() {
        var events = repository.findTop20ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (var event : events) {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY,
                    event.getPayload(),
                    message -> {
                        message.getMessageProperties().setMessageId(event.getId().toString());
                        message.getMessageProperties().setHeader("eventType", event.getEventType());
                        return message;
                    });
            event.markPublished();
        }
    }
}
