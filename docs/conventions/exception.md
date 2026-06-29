# Exception Convention

공통 예외는 `BaseException`을 상속하는 HTTP 상태별 예외 클래스로 표현하는 것을 기본 기준으로 합니다.

구현 위치: `com.msdragon.backend.common.exception`

---

## 예외 계층

```text
BaseException
├── BadRequestException
├── UnAuthorizedException
├── NotFoundException
└── InternalServerException
```

---

## 처리 방식

- 커스텀 예외는 `ControllerExceptionAdvice`에서 공통 실패 응답으로 변환합니다.
- 요청 파라미터 누락은 `MissingServletRequestParameterException`에서 처리합니다.
- Bean Validation 실패는 `MethodArgumentNotValidException`에서 처리합니다.
- 잘못된 인자는 `IllegalArgumentException`에서 처리합니다.
- 현재 공통 예외 응답에는 별도 에러 코드 enum을 포함하지 않습니다.

---

## 사용 예시

```kotlin
throw BadRequestException("잘못된 요청입니다.")
```

응답:

```json
{
  "status": 400,
  "success": false,
  "message": "잘못된 요청입니다."
}
```

---

## 추가 검토 항목

- 에러 코드 enum 또는 상태 객체를 사용할지 여부
- 공통 예외 테스트 기준
