package br.com.reinaldodev.smartbilling.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

/**
 * Consumidor didático com deduplicação simples via Redis.
 * Em produção, defina TTL, política de falhas/DLQ e idempotência persistente.
 */
@Component
public class InvoiceEventConsumer {
    private final StringRedisTemplate redis;

    public InvoiceEventConsumer(StringRedisTemplate redis) { this.redis = redis; }

    @RabbitListener(queues = "billing.invoice.created")
    public void onInvoiceCreated(String payload,
            org.springframework.amqp.core.Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null) {
            throw new IllegalStateException("Evento sem messageId");
        }
        String key = "processed:event:" + messageId;
        Boolean firstDelivery = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(7));
        if (Boolean.FALSE.equals(firstDelivery)) {
            return; // Mensagem já processada: comportamento idempotente simplificado.
        }
        // Aqui entraria a integração com CRM, notificações ou outro sistema.
        System.out.println("Evento de cobrança recebido: " + payload);
    }
}
