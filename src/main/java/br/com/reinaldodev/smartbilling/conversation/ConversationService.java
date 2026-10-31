package br.com.reinaldodev.smartbilling.conversation;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class ConversationService {
    private final StringRedisTemplate redis;
    private final Duration ttl;

    public ConversationService(StringRedisTemplate redis) {
        this.redis = redis;
        this.ttl = Duration.ofMinutes(120);
    }

    public void appendMessage(String conversationId, String role, String content) {
        String key = key(conversationId);
        redis.opsForList().rightPush(key, role + ": " + content);
        redis.expire(key, ttl);
    }

    public java.util.List<String> getHistory(String conversationId) {
        var history = redis.opsForList().range(key(conversationId), 0, -1);
        return history == null ? java.util.List.of() : history;
    }

    private String key(String id) { return "conversation:" + id + ":history"; }
}
