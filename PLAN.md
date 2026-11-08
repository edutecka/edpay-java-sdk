# Roadmap & Plano de Arquitetura - EDPay Java SDK

## Objectivos
1. Cliente HTTP moderno construído com a `java.net.http.HttpClient` do Java 17+ (zero dependências obrigatórias de terceiros).
2. Compatibilidade fluida com Spring Boot 3+ (Spring Boot Starter opcional).
3. Verificação HMAC SHA-256 em tempo constante com `MessageDigest.isEqual`.

---

## Fases de Desenvolvimento

### Fase 1: Core Client & Autenticação (Concluído)
- [x] Headers `X-EDPay-Public-Key` e `X-EDPay-Private-Key`.
- [x] Suporte a idempotência via `Idempotency-Key`.
- [x] Alternância entre ambientes `PRODUCTION` e `SANDBOX`.
- [x] `WebhookVerifier` com `HmacSHA256` e comparação segura.

### Fase 2: Módulos de Recursos (Concluído)
- [x] `PaymentsResource`: `create`, `retrieve`, `cancel`.
- [x] `CheckoutResource`: `createSession`, `retrieveSession`, `expireSession`.

### Fase 3: Spring Boot Starter (Em Planeamento)
- [ ] Auto-configuração `edpay-spring-boot-starter`.
- [ ] `@EnableEDPay` e injeção do Bean `@Autowired EDPayClient`.
- [ ] Controller base para webhook com annotations.

### Fase 4: Maven Central & CI/CD
- [ ] Workflow GitHub Actions para build com JDK 17 e JDK 21.
- [ ] Publicação no Sonatype / Maven Central.
