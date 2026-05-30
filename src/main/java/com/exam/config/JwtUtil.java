package com.exam.config;

import com.exam.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtUtil {
    private final String secret;
    private final ObjectMapper mapper;

    public JwtUtil(@Value("${app.jwt.secret}") String secret, ObjectMapper mapper) {
        this.secret = secret;
        this.mapper = mapper;
    }

    public String createToken(User user) {
        try {
            String header = base64Url(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = base64Url(mapper.writeValueAsBytes(Map.of(
                    "id", user.getId(),
                    "role", user.getRole().name(),
                    "name", user.getName(),
                    "exp", Instant.now().plusSeconds(24 * 60 * 60).getEpochSecond()
            )));
            String body = header + "." + payload;
            return body + "." + sign(body);
        } catch (Exception e) {
            throw new IllegalStateException("生成登录凭证失败");
        }
    }

    public boolean verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String body = parts[0] + "." + parts[1];
            if (!sign(body).equals(parts[2])) {
                return false;
            }
            Map<?, ?> payload = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            Number exp = (Number) payload.get("exp");
            return exp != null && exp.longValue() > Instant.now().getEpochSecond();
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
