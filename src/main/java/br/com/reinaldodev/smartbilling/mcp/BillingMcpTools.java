package br.com.reinaldodev.smartbilling.mcp;

import br.com.reinaldodev.smartbilling.billing.BillingService;
import br.com.reinaldodev.smartbilling.conversation.ConversationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Ferramentas expostas ao cliente MCP. O agente chama estas ferramentas;
 * elas delegam ao domínio/backend e não acessam o banco diretamente.
 */
@Component
public class BillingMcpTools {
    private final BillingService billing;
    private final ConversationService conversations;

    public BillingMcpTools(BillingService billing, ConversationService conversations) {
        this.billing = billing; this.conversations = conversations;
    }

    @Tool(name = "consultar_cobrancas_abertas",
            description = "Consulta as cobranças em aberto de um cliente pelo identificador interno.")
    public String consultarCobrancasAbertas(
            @ToolParam(description = "ID interno do cliente") Long customerId) {
        return billing.findOpenInvoices(customerId).stream()
                .map(i -> "Cobrança #" + i.getId() + " | R$ " + i.getAmount()
                        + " | vencimento " + i.getDueDate() + " | status " + i.getStatus())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("Nenhuma cobrança em aberto.");
    }

    @Tool(name = "consultar_cobranca",
            description = "Consulta os detalhes de uma cobrança pelo identificador.")
    public String consultarCobranca(
            @ToolParam(description = "ID da cobrança") Long invoiceId) {
        var invoice = billing.findById(invoiceId);
        return "Cobrança #" + invoice.getId()
                + " | cliente " + invoice.getCustomer().getName()
                + " | valor R$ " + invoice.getAmount()
                + " | vencimento " + invoice.getDueDate()
                + " | status " + invoice.getStatus();
    }

    @Tool(name = "registrar_contexto_atendimento",
            description = "Registra uma mensagem no histórico de uma conversa para permitir continuidade do atendimento.")
    public String registrarContexto(
            @ToolParam(description = "Identificador da conversa") String conversationId,
            @ToolParam(description = "Papel: USER, ASSISTANT ou HUMAN_AGENT") String role,
            @ToolParam(description = "Conteúdo da mensagem") String content) {
        conversations.appendMessage(conversationId, role, content);
        return "Contexto registrado para a conversa " + conversationId;
    }

    @Tool(name = "recuperar_contexto_atendimento",
            description = "Recupera o histórico de uma conversa antes de continuar ou transferir o atendimento.")
    public String recuperarContexto(
            @ToolParam(description = "Identificador da conversa") String conversationId) {
        return String.join("\n", conversations.getHistory(conversationId));
    }
}
