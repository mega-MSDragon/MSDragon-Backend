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
