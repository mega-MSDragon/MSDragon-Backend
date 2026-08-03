# Exception Convention

공통 예외는 `BaseException`을 상속합니다. `BaseException.status`는 응답 본문의 앱 상태 코드이고, `BaseException.httpStatus`는 실제 HTTP 상태입니다.

구현 위치: `com.msdragon.backend.common.exception`

---

## 예외 계층

```text
BaseException
├── BadRequestException
├── UnAuthorizedException
├── ForbiddenException
├── NotFoundException
└── InternalServerException
```

---

## 처리 방식

- 커스텀 예외는 `ControllerExceptionAdvice`에서 공통 실패 응답으로 변환합니다.
- `BadRequestException`, `UnAuthorizedException`, `ForbiddenException`, `NotFoundException`은 서버가 처리할 수 있는 예상 가능한 오류입니다. 실제 HTTP는 `200`이고, 각 의미에 맞는 값을 본문 `status`로 반환합니다.
- 보호 API의 access token 누락·형식·만료·변조 오류도 `UnAuthorizedException`으로 처리하며 HTTP `200`, 본문 `status=401`을 반환합니다.
- 서버 설정, 외부 API, PDF 생성 등 시스템 실패는 `InternalServerException`으로 처리하며 실제 HTTP `500`을 반환합니다.
- 요청 파라미터 누락·형식 오류, Bean Validation 실패, JSON 본문 파싱 실패, 지원하지 않는 enum은 HTTP `200`, 본문 `status=400`으로 반환합니다.
- 현재 `status`는 정수형이며 별도 에러 코드 enum은 사용하지 않습니다.
- 성공 응답이 PDF 같은 바이너리인 API도 처리 가능한 실패 응답은 HTTP `200`, `Content-Type: application/json`인 공통 실패 응답을 반환합니다.

---

## 사용 예시

```kotlin
throw BadRequestException("잘못된 요청입니다.")
```

응답:

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "status": 400,
  "success": false,
  "message": "잘못된 요청입니다."
}
```

---

## 추가 검토 항목

- 도메인별 세부 비즈니스 코드가 필요해질 때 `status` 코드 목록과 enum을 도입할지 여부
- 공통 예외 테스트 기준
