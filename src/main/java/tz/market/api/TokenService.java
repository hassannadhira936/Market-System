package tz.market.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Token ndogo iliyosainiwa (HMAC-SHA256). Inachukua nafasi ya PHP $_SESSION. */
@Component
public class TokenService {
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private final byte[] secret;

    public TokenService(@Value("${app.jwt-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String create(AuthUser u) {
        long exp = Instant.now().plusSeconds(8 * 3600).getEpochSecond();
        String payload = u.userId() + "|" + u.roleId() + "|" + clean(u.username()) + "|" + clean(u.name()) + "|" + exp;
        String p = ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return p + "." + sign(p);
    }

    public Optional<AuthUser> verify(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot < 1) return Optional.empty();
            String p = token.substring(0, dot);
            byte[] given = token.substring(dot + 1).getBytes(StandardCharsets.UTF_8);
            byte[] expected = sign(p).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(given, expected)) return Optional.empty();
            String[] parts = new String(DEC.decode(p), StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 5 || Long.parseLong(parts[4]) < Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(new AuthUser(Long.parseLong(parts[0]), Integer.parseInt(parts[1]), parts[2], parts[3]));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return ENC.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String clean(String s) { return s == null ? "" : s.replace("|", ""); }
}
