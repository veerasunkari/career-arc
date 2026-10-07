package local.careerarc.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 160) public String email;
    @Column(nullable = false) public String passwordHash;
    @Column(name = "user_role", nullable = false, length = 24) public String role;
    @Column(nullable = false, length = 120) public String name;
    @Column(nullable = false) public boolean enabled = true;
    @Column(nullable = false) public Instant createdAt = Instant.now();
    public Account() {}
    public Account(String email, String passwordHash, String role, String name) {
        this.email = email; this.passwordHash = passwordHash; this.role = role; this.name = name;
    }
}