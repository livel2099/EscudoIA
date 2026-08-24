package com.livel.escudo.auth;

import com.livel.escudo.audit.AuditService;
import com.livel.escudo.common.ApiException;
import com.livel.escudo.user.UserEntity;
import com.livel.escudo.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final AuditService audit;

    public AuthController(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwords,
                          TokenService tokens, AuditService audit) {
        this.users = users; this.refreshTokens = refreshTokens; this.passwords = passwords; this.tokens = tokens; this.audit = audit;
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public TokenResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new ApiException(HttpStatus.CONFLICT, "EMAIL_IN_USE", "Ese correo ya está registrado.");
        UserEntity user = users.save(new UserEntity(email, passwords.encode(request.password())));
        audit.record(user.getId(), "USER_REGISTERED", "USER", user.getId(), http, "SUCCESS");
        return issue(user, http.getHeader("User-Agent"));
    }

    @PostMapping("/login") @Transactional
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        UserEntity user = users.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null || !passwords.matches(request.password(), user.getPasswordHash()) || !"ACTIVE".equals(user.getStatus())) {
            audit.record(null, "LOGIN_FAILED", "USER", null, http, "DENIED");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Correo o contraseña incorrectos.");
        }
        audit.record(user.getId(), "LOGIN_SUCCEEDED", "USER", user.getId(), http, "SUCCESS");
        return issue(user, http.getHeader("User-Agent"));
    }

    @PostMapping("/refresh") @Transactional
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        RefreshTokenEntity old = refreshTokens.findByTokenHash(tokens.hash(request.refreshToken()))
                .filter(RefreshTokenEntity::active)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "La sesión venció. Volvé a iniciar sesión."));
        old.revoke();
        UserEntity user = users.findById(old.getUserId()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "La sesión no es válida."));
        return issue(user, http.getHeader("User-Agent"));
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void logout(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        refreshTokens.findByTokenHash(tokens.hash(request.refreshToken())).ifPresent(token -> {
            token.revoke(); audit.record(token.getUserId(), "REFRESH_REVOKED", "USER", token.getUserId(), http, "SUCCESS");
        });
    }

    private TokenResponse issue(UserEntity user, String device) {
        String refresh = tokens.newRefreshToken();
        refreshTokens.save(new RefreshTokenEntity(user.getId(), tokens.hash(refresh), tokens.refreshExpiry(), device));
        return new TokenResponse(tokens.accessToken(user), refresh, "Bearer", user.getId().toString(), user.getEmail(), user.getRoles());
    }

    public record RegisterRequest(@NotBlank @Email String email, @NotBlank @Size(min=10, max=128) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken, String tokenType, String userId, String email, Set<String> roles) {}
}
