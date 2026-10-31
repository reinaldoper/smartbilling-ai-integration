# SmartBilling AI Integration

Projeto didático para estudar integração entre agentes de IA e sistemas corporativos de cobrança, alinhado aos temas de uma vaga Backend Sênior.

## O que este projeto demonstra

- Java 17 + Spring Boot 3.5
- APIs REST para clientes e cobranças
- PostgreSQL com Spring Data JPA
- Redis para histórico/contexto de conversas e deduplicação simples
- RabbitMQ para eventos assíncronos
- Transactional Outbox: cobrança e evento são persistidos na mesma transação
- MCP Server com ferramentas para consultar cobranças e recuperar/registrar contexto
- Perfil de OAuth2 Resource Server para validar JWT emitido por um provedor externo
- Perfil de mTLS para exigir certificado cliente no HTTPS
- Actuator para health checks e métricas básicas

> É um laboratório de estudo, não uma solução pronta para produção. Não há integração real com Salesforce/Amdocs, OCI Streaming, provedor LLM, nem fluxo completo de pagamento. O consumidor RabbitMQ simula o próximo sistema corporativo.

## Arquitetura

```text
                    +-----------------------+
                    |  Agente / MCP Client  |
                    +-----------+-----------+
                                |
                         MCP Streamable HTTP
                                |
                    +-----------v-----------+
                    | Spring AI MCP Server  |
                    | BillingMcpTools       |
                    +-----------+-----------+
                                |
                         Serviços de domínio
                                |
             +------------------+------------------+
             |                  |                  |
       +-----v------+     +-----v------+     +-----v------+
       | PostgreSQL |     |    Redis   |     | RabbitMQ   |
       | customers  |     | conversa   |     | eventos    |
       | invoices   |     | dedup      |     +-----+------+
       | outbox     |     +------------+           |
       +-----+------+                              v
             |                               Consumer / CRM
             +-- Outbox Publisher ----------> (simulado)
```

## Pré-requisitos

- JDK 17 ou superior
- Maven 3.9+
- Docker Engine + Docker Compose
- IDE de sua preferência (IntelliJ, VS Code ou Eclipse)

## 1. Subir a infraestrutura

```bash
docker compose up -d
docker compose ps
```

RabbitMQ Management: http://localhost:15672  
Usuário: `smartbilling`  
Senha: `smartbilling_dev`

## 2. Executar a aplicação

```bash
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

## 3. Criar um cliente

```bash
curl -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Maria da Silva",
    "document": "12345678900",
    "email": "maria@example.com"
  }'
```

Guarde o `id` retornado.

## 4. Criar uma cobrança

Substitua `1` pelo ID do cliente:

```bash
curl -X POST http://localhost:8080/api/invoices \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": 1,
    "amount": 249.90,
    "dueDate": "2026-11-15"
  }'
```

A operação grava a cobrança e o evento na tabela `outbox_events` dentro da mesma transação. O publisher agenda o envio ao RabbitMQ.

## 5. Consultar cobranças

```bash
curl http://localhost:8080/api/invoices/customer/1
curl http://localhost:8080/api/invoices/customer/1/open
```

## 6. Testar o contexto de conversa

```bash
curl -X POST http://localhost:8080/api/conversations/chat-001/messages \
  -H 'Content-Type: application/json' \
  -d '{"role":"USER","content":"Quero consultar minha fatura."}'

curl -X POST http://localhost:8080/api/conversations/chat-001/messages \
  -H 'Content-Type: application/json' \
  -d '{"role":"ASSISTANT","content":"Informe seu identificador de cliente."}'

curl http://localhost:8080/api/conversations/chat-001
```

## 7. MCP

O servidor MCP é configurado para Streamable HTTP. Com a aplicação iniciada, o endpoint padrão é:

```text
http://localhost:8080/mcp
```

Ferramentas expostas:

- `consultar_cobrancas_abertas(customerId)`
- `consultar_cobranca(invoiceId)`
- `registrar_contexto_atendimento(conversationId, role, content)`
- `recuperar_contexto_atendimento(conversationId)`

Conecte um cliente MCP compatível ao endpoint. Para inspecionar ferramentas, use o MCP Inspector. O protocolo MCP não é uma API REST comum: a comunicação utiliza JSON-RPC sobre o transporte MCP.

**Segurança:** a configuração local permite acesso sem autenticação para facilitar o laboratório. Não exponha esse endpoint na rede/Internet assim. O próprio transporte MCP não ativa autenticação automaticamente. Antes de uso externo, coloque Spring Security/API Gateway na frente e proteja também as ferramentas e os dados por usuário/tenant.

## 8. Perfil OAuth2 (validação de JWT)

O perfil `oauth2` transforma a API em Resource Server. Ele valida tokens JWT emitidos por um provedor de identidade externo (por exemplo, Keycloak).

Configure `OAUTH_ISSUER_URI` com o issuer real do seu realm:

```bash
export OAUTH_ISSUER_URI=http://localhost:8081/realms/smartbilling
SPRING_PROFILES_ACTIVE=oauth2 mvn spring-boot:run
```

Ajuste a URL conforme o provedor utilizado. Para chamar endpoints protegidos:

```bash
curl http://localhost:8080/api/customers \
  -H "Authorization: Bearer SEU_ACCESS_TOKEN"
```

Este projeto não inclui o servidor de identidade. O próximo exercício recomendado é subir Keycloak via Docker Compose, criar realm, client, usuário e roles, e exigir scopes/roles nas operações sensíveis.

## 9. Perfil mTLS

O perfil `mtls` exige certificado cliente na conexão HTTPS. Gere certificados de laboratório com uma CA própria e configure o certificado do servidor e o truststore com a CA que assina os certificados clientes.

Exemplo de criação de uma CA e certificado de servidor (ambiente local):

```bash
mkdir -p certs

openssl req -x509 -newkey rsa:4096 -keyout certs/ca.key -out certs/ca.crt \
  -days 365 -nodes -subj "/CN=SmartBilling-Dev-CA"

openssl req -newkey rsa:2048 -nodes -keyout certs/server.key \
  -out certs/server.csr -subj "/CN=localhost"
openssl x509 -req -in certs/server.csr -CA certs/ca.crt -CAkey certs/ca.key \
  -CAcreateserial -out certs/server.crt -days 365 -sha256 \
  -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1")

openssl pkcs12 -export -in certs/server.crt -inkey certs/server.key \
  -certfile certs/ca.crt -out certs/server.p12 -name server -passout pass:changeit

keytool -importcert -noprompt -alias dev-ca -file certs/ca.crt \
  -keystore certs/truststore.p12 -storetype PKCS12 -storepass changeit
```

Para testar mTLS de ponta a ponta, crie também um certificado de cliente assinado pela mesma CA, exporte-o para PKCS12 e use `curl --cert client.crt --key client.key`. O certificado do cliente deve ser confiável no truststore do servidor.

```bash
SPRING_PROFILES_ACTIVE=mtls mvn spring-boot:run
```

Variáveis opcionais: `TLS_KEYSTORE`, `TLS_KEYSTORE_PASSWORD`, `TLS_TRUSTSTORE`, `TLS_TRUSTSTORE_PASSWORD`.

Não comite chaves privadas nem certificados de produção. Os comandos acima são somente para laboratório.

## 10. Como estudar o Outbox corretamente

O fluxo implementado é:

1. `BillingService.createInvoice()` inicia uma transação.
2. Salva a cobrança.
3. Salva o evento Outbox na mesma transação.
4. Se qualquer gravação falhar, ambas são revertidas.
5. `OutboxPublisher` busca eventos pendentes e publica no RabbitMQ.
6. O consumidor recebe o evento e faz deduplicação simples com Redis.

**Ponto de arquitetura para entrevista:** este relay didático ainda tem uma janela de duplicidade entre publicar no broker e marcar o evento como publicado. Em produção, implemente publisher confirms, estratégia de claim/lock do lote, retry/backoff, DLQ e idempotência persistente no consumidor. O Outbox normalmente oferece entrega *at-least-once*, não exatamente uma vez.

## Plano de estudos sugerido

### Etapa 1 — APIs e domínio
- Entender controllers, services, repositories e entidades.
- Criar clientes e cobranças.
- Validar dados e tratar erros com `@ControllerAdvice`.

### Etapa 2 — Outbox e mensageria
- Colocar breakpoint na transação de criação.
- Verificar a tabela `outbox_events`.
- Derrubar o RabbitMQ e observar o comportamento.
- Melhorar o relay com publisher confirms e lock.
- Implementar DLQ e retry.

### Etapa 3 — Redis e continuidade de contexto
- Registrar mensagens com um conversationId.
- Simular atendimento automatizado e transferência humana.
- Implementar expiração e isolamento por tenant.

### Etapa 4 — MCP
- Conectar o MCP Inspector.
- Listar as ferramentas disponíveis.
- Executar consultas de cobrança.
- Adicionar uma ferramenta de segunda via com validação/autorização.

### Etapa 5 — Segurança
- Integrar Keycloak e OAuth2/JWT.
- Criar roles e scopes.
- Gerar certificados X.509 e ativar mTLS.
- Restringir chamadas entre serviços.

### Etapa 6 — Evolução para a vaga
- Substituir RabbitMQ por OCI Streaming.
- Criar um adaptador de CRM (Salesforce/Amdocs simulado).
- Adicionar gRPC entre dois serviços.
- Adicionar observabilidade com Micrometer, Prometheus e Grafana.

## Perguntas que você deve conseguir responder depois

1. Por que usar Outbox em vez de publicar diretamente no RabbitMQ dentro do service?
2. Qual a diferença entre entrega *at-least-once* e *exactly-once*?
3. Como evitar processar o mesmo evento duas vezes?
4. Como preservar o contexto quando o atendimento muda de agente para humano?
5. Qual a diferença entre REST e gRPC?
6. Qual a diferença entre OAuth2, JWT e mTLS?
7. O que é MCP e por que não expor acesso direto ao banco para o agente?
8. Como garantir isolamento de dados entre clientes/tenants?

## Documentação oficial

- Spring Boot: https://docs.spring.io/spring-boot/
- Spring AI / MCP: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- RabbitMQ com Spring: https://docs.spring.io/spring-boot/reference/messaging/amqp.html
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Redis: https://redis.io/docs/latest/
