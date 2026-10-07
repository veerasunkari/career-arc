package local.careerarc.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_token", indexes = @Index(name = "idx_refresh_token_family", columnList = "family_id"))
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) public String tokenHash;
    @Column(name = "family_id", nullable = false, length = 36) public String familyId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id", nullable = false)
    public Account account;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
    @Column(name = "used_at") public Instant usedAt;
    @Column(name = "revoked_at") public Instant revokedAt;

    protected RefreshToken() {}

    public RefreshToken(String tokenHash, String familyId, Account account, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.account = account;
        this.expiresAt = expiresAt;
    }
}
