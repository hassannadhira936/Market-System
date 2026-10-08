package tz.market.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenService tokens;

    public AuthInterceptor(TokenService tokens) { this.tokens = tokens; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new ApiException(401, "Ingia kwanza (login).");
        Optional<AuthUser> u = tokens.verify(h.substring(7));
        if (u.isEmpty()) throw new ApiException(401, "Session imeisha. Ingia tena.");
        // Role 10 = vendor. Portal ya vendor bado haijahamishwa.
        if (u.get().roleId() == 10) throw new ApiException(403, "Portal ya vendor bado haijahamishwa kwenda React.");
        req.setAttribute("user", u.get());
        return true;
    }
}
