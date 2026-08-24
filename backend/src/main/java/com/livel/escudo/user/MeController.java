package com.livel.escudo.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserRepository users;
    public MeController(UserRepository users) { this.users = users; }
    @GetMapping public Profile get(@AuthenticationPrincipal UserEntity user) { return profile(user); }
    @PutMapping @Transactional public Profile update(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody UpdateProfile request) {
        user.setLocale(request.locale()); return profile(users.save(user));
    }
    private Profile profile(UserEntity user) { return new Profile(user.getId(), user.getEmail(), user.getStatus(), user.getLocale(), user.getRoles()); }
    public record UpdateProfile(@Pattern(regexp="[a-z]{2}-[A-Z]{2}") String locale) {}
    public record Profile(UUID id, String email, String status, String locale, Set<String> roles) {}
}

