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
