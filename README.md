# EDPay Java SDK

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

SDK oficial da EDPay para integração de pagamentos em Java e ecossistema JVM (Spring Boot, Quarkus, Micronaut e Java SE).

---

## Instalação

### Maven
Adicione ao seu `pom.xml`:
```xml
<dependency>
    <groupId>mz.co.edpay</groupId>
    <artifactId>edpay-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'mz.co.edpay:edpay-java-sdk:1.0.0'
```

---

## Inicialização

```java
import mz.co.edpay.EDPayClient;

EDPayClient client = new EDPayClient.Builder()
    .publicKey("pk_live_...")
    .privateKey("sk_live_...")
    .environment(EDPayClient.Environment.PRODUCTION) // ou SANDBOX
    .build();
```

---

## Criar um Pagamento Direto (API Checkout)

```java
import mz.co.edpay.resources.PaymentRequest;
import mz.co.edpay.resources.PaymentResponse;

PaymentRequest request = PaymentRequest.builder()
    .amountMinor(50000L)                 // 500.00 MZN
    .currency("MZN")
    .method("emola")                     // ou "mpesa", "card", "apple_pay", etc.
    .phone("+25884xxxxxxx")
    .reference("FAT-2026-001")
    .description("Cobrança Mensalidade")
    .emolaCustomMessage("Pagamento de Serviços via EDPay")
    .build();

PaymentResponse response = client.payments().create(request);

System.out.println("ID: " + response.getId());
System.out.println("Status: " + response.getStatus());
```

---

## Verificar Assinatura de Webhooks

A EDPay assina requisições de webhook com HMAC SHA-256 no header `X-EDPay-Signature`.

```java
import mz.co.edpay.webhooks.WebhookVerifier;

boolean isValid = WebhookVerifier.verify(
    rawJsonPayload,
    requestHeaderSignature,
    "whsec_..."
);

if (!isValid) {
    throw new SecurityException("Assinatura de Webhook inválida.");
}
```

---

## Condições Comerciais

- **Tarifa Unificada**: 6,00% em Meticais (MZN).
- **Liquidação Bancária**: Transferência automática de 2 a 7 dias úteis.

---

## Licença

MIT © [EDPay](https://edpay.co.mz)
