# Health API

서버 실행 상태를 확인하는 기본 API입니다.

---

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/health` | 서버 상태 확인 |

---

## GET /health

### Response

```json
{
  "status": "UP"
}
```
