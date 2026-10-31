package br.com.reinaldodev.smartbilling.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "billing.events";
    public static final String QUEUE = "billing.invoice.created";
    public static final String ROUTING_KEY = "invoice.created";

    @Bean
    DirectExchange billingExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue invoiceCreatedQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding invoiceCreatedBinding() {
        return BindingBuilder.bind(invoiceCreatedQueue())
                .to(billingExchange())
                .with(ROUTING_KEY);
    }
}
