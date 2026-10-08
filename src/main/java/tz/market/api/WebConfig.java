package tz.market.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor auth;
    private final String[] origins;

    public WebConfig(
            AuthInterceptor auth,
            @Value("${app.cors-origins}") String origins
    ) {
        this.auth = auth;
        this.origins = origins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry r) {
        r.addMapping("/api/**")
            .allowedOrigins(origins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry r) {
        r.addInterceptor(auth)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/vendor-register"
            );
    }
}