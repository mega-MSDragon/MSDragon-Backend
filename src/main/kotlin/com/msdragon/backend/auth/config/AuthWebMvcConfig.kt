package com.msdragon.backend.auth.config

import com.msdragon.backend.auth.support.AuthInterceptor
import com.msdragon.backend.auth.support.CurrentUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AuthWebMvcConfig(
	private val authInterceptor: AuthInterceptor,
	private val currentUserArgumentResolver: CurrentUserArgumentResolver,
) : WebMvcConfigurer {
	override fun addInterceptors(registry: InterceptorRegistry) {
		registry.addInterceptor(authInterceptor)
			.addPathPatterns("/api/v1/**")
			.excludePathPatterns("/api/v1/auth/**")
	}

	override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
		resolvers.add(currentUserArgumentResolver)
	}
}
