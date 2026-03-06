# Implementation Plan: Tokenized QR + Persisted Label Registration

## Corrected Workflow Assumptions

### 1) Label Registration – Backend-Persisted
- **Intended flow**: Register label → persist in backend (`qr_labels`) → lookup → ZPL → print → scan → status → audit
- **Current gap**: `RegisterLabelPage` does local-only generation; does not call `POST /api/v1/label`
- **Fix**: RegisterLabelPage must call `POST /api/v1/label`, persist first, then generate QR from response

### 2) Canonical QR Format
- **Format**: `OLNQR:1:<public_token>`
- **No** JSON, plain lote, or mixed formats for newly generated QR codes
- **Backward compatibility**: LoteExtractor continues to support legacy JSON/URL/plain for existing printed labels

### 3) Security Boundary
- **Not** the public `/qr/{...}` HTML landing
- **Real requirements**:
  - No sensitive data without authentication
  - `/api/v1/qr/*` and token lookup require auth
  - External users may see opaque token string, but cannot retrieve operational data (lote, status, etc.) without auth

---

## Implementation Tasks

### Phase 1: Backend – Token Storage & API

| Task | Description |
|------|-------------|
| 1.1 | Add `public_token` column to `qr_labels` (V7 migration), backfill existing rows |
| 1.2 | Add `publicToken` to `QrLabel` entity and `QrLabelRepository.findByPublicToken()` |
| 1.3 | `LabelController.create`: Generate UUID as `public_token`, persist, return in `CreateResponse` |
| 1.4 | `LabelDto.CreateResponse`: Add `publicToken` field |
| 1.5 | `QrDto.Label`: Add `publicToken` (for GenerateQrPage) |
| 1.6 | `LoteExtractor`: Parse `OLNQR:1:<token>`, return token |
| 1.7 | `QrQueryService`: Resolve input – try `findByPublicToken` first, then `findByLote` |
| 1.8 | `LabelController.downloadZpl`: Resolve by token (findByPublicToken) or id/lote |

### Phase 2: Web – RegisterLabelPage

| Task | Description |
|------|-------------|
| 2.1 | Call `POST /api/v1/label` with form data (map fechaTipo/fechaValor to caducidad/reanalisis) |
| 2.2 | Use `publicToken` from response, encode `OLNQR:1:<publicToken>` in QR |
| 2.3 | Download PNG only after successful persist |
| 2.4 | Update description: "Registra etiqueta en el sistema y genera QR imprimible" |

### Phase 3: Web – GenerateQrPage

| Task | Description |
|------|-------------|
| 3.1 | Use `publicToken` from `QrResponse.label` when available |
| 3.2 | Encode `OLNQR:1:<publicToken>` in QR (not plain lote) |
| 3.3 | Fallback to `OLNQR:1:<lote>` for legacy labels without publicToken |

### Phase 4: LoteExtractor (Web/Android)

| Task | Description |
|------|-------------|
| 4.1 | Java `LoteExtractor`: Add `OLNQR:1:<token>` parsing, return token |
| 4.2 | Kotlin `LoteExtractor`: Same |
| 4.3 | API accepts raw scan; backend resolves token or lote |

---

## QR Payload Summary

| Scenario | Payload |
|----------|---------|
| **New labels** (after implementation) | `OLNQR:1:<public_token>` |
| **Legacy labels** (no public_token) | Continue accepting JSON, URL, plain lote via LoteExtractor |
| **GenerateQrPage** (existing DB labels with token) | `OLNQR:1:<publicToken>` |

---

## API Changes

### `POST /api/v1/label` CreateResponse (extended)
```json
{
  "id": "uuid",
  "status": "PENDING",
  "qrUrl": "https://host/qr/{id}",
  "publicToken": "uuid",
  "label": { ... }
}
```

### `GET /api/v1/qr/{loteOrToken}`
- Input: lote, token, or `OLNQR:1:<token>`
- Resolves: `findByPublicToken(token)` or `findByLote(lote)`
- Returns: `QrResponse` with `label.publicToken` when available

### `GET /api/v1/label/{id|lote|token}/zpl`
- Already supports id and lote
- Extended: resolve by `public_token`

---

## Implementation Status (Completed)

- [x] V7 migration: `public_token` column, backfill
- [x] QrLabel entity + repository `findByPublicToken`
- [x] LabelController create: generate token, return in CreateResponse
- [x] QrDto.Label + LabelDto.LabelView: publicToken
- [x] LoteExtractor (Java, Kotlin): OLNQR:1:\<token\> parsing
- [x] QrQueryService: resolve by token or lote
- [x] LabelController ZPL + PATCH by-lote: resolve by token
- [x] ScanController: resolve identifier to lote for POST/GET
- [x] RegisterLabelPage: POST /label, encode OLNQR:1:publicToken
- [x] GenerateQrPage: encode OLNQR:1:publicToken when available
