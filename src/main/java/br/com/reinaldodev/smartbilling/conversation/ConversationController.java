package br.com.reinaldodev.smartbilling.conversation;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService service;
    public ConversationController(ConversationService service) { this.service = service; }

    @PostMapping("/{conversationId}/messages")
    public void append(@PathVariable String conversationId, @RequestBody MessageRequest request) {
        service.appendMessage(conversationId, request.role(), request.content());
    }

    @GetMapping("/{conversationId}")
    public List<String> history(@PathVariable String conversationId) {
        return service.getHistory(conversationId);
    }

    public record MessageRequest(@NotBlank String role, @NotBlank String content) {}
}
