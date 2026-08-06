# Swagger Convention

API를 추가하거나 수정할 때 Swagger 문서화 어노테이션을 함께 작성합니다.
사용자가 별도로 요청하지 않아도 API 작업에는 Swagger 문서화를 포함합니다.

---

## 필수 어노테이션

컨트롤러 클래스:

```kotlin
@Tag(name = "Sample", description = "Sample API 입니다.")
```

엔드포인트:

```kotlin
@Operation(summary = "샘플 API", description = "샘플 데이터를 조회합니다.")
@ApiResponses(
	value = [
		SwaggerApiResponse(responseCode = "200", description = "샘플 조회 성공"),
	],
)
```

정책 오류가 있는 API는 실제 HTTP `200` 응답 설명에 본문 `status` 목록을 함께 적습니다.

```kotlin
@ApiResponses(
	value = [
		SwaggerApiResponse(
			responseCode = "200",
			description = "처리 완료: 성공(status=200) 또는 요청·인증·정책 오류(status=400/401/403/404)",
		),
		SwaggerApiResponse(responseCode = "500", description = "서버 또는 외부 연동 오류"),
	],
)
```

- `responseCode`는 본문 `status`가 아니라 실제 HTTP 상태만 적습니다.
- 요청·Validation·인증·정책상 `400/401/403/404`는 별도 HTTP 응답으로 나열하지 않고 HTTP `200` 설명에 포함합니다.
- HTTP `500`은 필수 설정 누락, 외부 API 호출 실패 등 실제 시스템 오류가 가능한 API에만 적습니다.
- 생성 성공은 HTTP `200`, 본문 `status=201`로 문서화합니다.
- `OpenApiConfig`는 `description`의 `status=200/400/...` 표기를 읽어 HTTP `200` 응답의 `application/json`에 이름 있는 예시를 자동으로 추가합니다.
- Springdoc이 공통 응답 schema를 `*/*`로 생성한 경우 `application/json`으로 정규화해 schema와 예시를 같은 media type에 표시합니다.
- Swagger UI에서는 `200` 응답의 Example 드롭다운에서 성공, 요청 오류, 인증 오류, 권한 오류, 조회 오류 등을 선택해 확인합니다.
- 실제 HTTP `500` 응답에는 서버 오류 예시를 자동으로 추가합니다.
- 바이너리 응답은 `application/pdf` 등 원래 media type을 유지하고 오류용 `application/json`에만 내부 status 예시를 추가합니다.

파라미터:

```kotlin
@Parameter(description = "응답으로 반환할 샘플 데이터", example = "hello")
```

DTO:

```kotlin
@Schema(description = "샘플 데이터")
data class SampleData(
	@field:Schema(description = "샘플 값", example = "hello")
	val value: String,
)
```

Enum DTO 필드:

```kotlin
@field:Schema(
	description = "소셜 로그인 provider",
	example = "kakao",
	allowableValues = ["kakao", "apple"],
)
val provider: OAuthProvider
```

- 클라이언트가 Swagger UI만 보고 구현할 수 있도록 enum 필드는 `allowableValues`와 `example`을 함께 작성합니다.
- JSON 값은 Kotlin enum 이름이 아니라 API에서 실제로 주고받는 문자열을 적습니다. 예: `KAKAO`가 아니라 `kakao`.
- 역할별 허용 범위처럼 단순 enum 목록 외 검증 조건이 있으면 필드 설명이나 API 문서에 추가로 적습니다.

보호 API:

```kotlin
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
class SampleController
```

- `Authorization: Bearer {accessToken}`이 필요한 컨트롤러에는 `@SecurityRequirement`를 붙입니다.
- OpenAPI 보안 스키마 이름은 `common.config.BEARER_AUTH_SCHEME`의 `bearerAuth`를 사용합니다.
- `/api/v1/auth/**`처럼 로그인 전 호출해야 하는 공개 API에는 보안 요구사항을 붙이지 않습니다.
- `@CurrentUser`처럼 서버에서 주입하는 파라미터는 클라이언트 요청값이 아니므로 `@Parameter(hidden = true)`로 Swagger에서 숨깁니다.

---

## 이름 충돌 규칙

프로젝트의 `ApiResponse` 클래스와 Swagger의 `ApiResponse` 어노테이션 이름이 겹치므로 Swagger 어노테이션은 alias import를 사용합니다.

```kotlin
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
```

---

## 검증

- `./gradlew test`로 컴파일을 확인합니다.
- Swagger UI를 사용한다면 `/swagger-ui.html`에서 태그, 요약, 설명, 응답 코드 노출을 확인합니다.
- enum 요청값은 `/v3/api-docs`의 schema `enum` 배열에 실제 JSON 값으로 노출되는지 확인합니다.
- 보호 API는 `/v3/api-docs`의 `components.securitySchemes.bearerAuth`와 operation `security`에 노출되는지 확인합니다.
