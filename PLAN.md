# Roadmap & Architecture Plan - EDPay Java SDK

## Objectives
1. Modern HTTP client built with Java 17+'s `java.net.http.HttpClient` (zero mandatory third-party dependencies).
2. Seamless compatibility with Spring Boot 3+ (optional Spring Boot Starter).
3. Constant-time HMAC SHA-256 verification with `MessageDigest.isEqual`.

---

## Development Phases

### Phase 1: Core Client & Authentication (Completed)
- [x] Headers `X-EDPay-Public-Key` and `X-EDPay-Private-Key`.
- [x] Idempotency support via `Idempotency-Key`.
- [x] Switching between `PRODUCTION` and `SANDBOX` environments.
- [x] `WebhookVerifier` for timestamped `Payment-Signature` HMAC-SHA256 headers, constant-time comparison, and a configurable replay window.

### Phase 2: API Resource Coverage (Completed in 1.4.0)
- [x] Payment records with private, hosted, and link checkout operations; customers, invoices, and invoice series.
- [x] FX quotes, wallets and cursor-paginated ledger entries, public payment methods, and webhooks.
- [x] Method coverage follows mounted routes; the API has no payment-cancel, session-expire, or invoice-series-detail route.
- [x] Method-aware contacts, public endpoint calls, API error metadata, and timestamped webhook verification.

### Phase 3: Spring Boot Starter (Planned)
- [ ] Auto-configuration for `edpay-spring-boot-starter`.
- [ ] `@EnableEDPay` and `@Autowired EDPayClient` bean injection.
- [ ] Base webhook controller with annotations.

### Phase 4: Maven Central & CI/CD
- [ ] GitHub Actions workflow to build with JDK 17 and JDK 21.
- [ ] Publication on Sonatype / Maven Central.
