package com.livel.escudo.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private String locale;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_code")
    private Set<String> roles = new HashSet<>();

    protected UserEntity() {}
    public UserEntity(String email, String passwordHash) {
        this.id = UUID.randomUUID(); this.email = email; this.passwordHash = passwordHash;
        this.status = "ACTIVE"; this.locale = "es-AR"; this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
        this.roles.add("ROLE_USER");
    }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getStatus() { return status; }
    public String getLocale() { return locale; }
    public Set<String> getRoles() { return Set.copyOf(roles); }
    public void setLocale(String locale) { this.locale = locale; this.updatedAt = Instant.now(); }
}

