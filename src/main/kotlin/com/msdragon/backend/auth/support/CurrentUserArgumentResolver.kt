package com.msdragon.backend.auth.support

import com.msdragon.backend.common.exception.UnAuthorizedException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
	override fun supportsParameter(parameter: MethodParameter): Boolean =
		parameter.hasParameterAnnotation(CurrentUser::class.java) &&
			parameter.parameterType == AuthenticatedUser::class.java

	override fun resolveArgument(
		parameter: MethodParameter,
		mavContainer: ModelAndViewContainer?,
		webRequest: NativeWebRequest,
		binderFactory: WebDataBinderFactory?,
	): Any {
		return webRequest.getAttribute(AuthRequestAttributes.CURRENT_USER, RequestAttributes.SCOPE_REQUEST)
			?: throw UnAuthorizedException("인증 사용자 정보를 찾을 수 없습니다.")
	}
}
