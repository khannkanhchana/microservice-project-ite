package kh.edu.istad.iam.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "identities")
@Getter
@Setter
@NoArgsConstructor
public class Identity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 20)
    private String provider;
    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;
    @Column(length = 255)
    private String email;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    @Column(name = "display_name", length = 150)
    private String displayName;
    @Column(length = 100)
    private String username;
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes;
    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Column(name = "deleted_by", length = 255)
    private String deletedBy;
    @Version
    @Column(nullable = false)
    private long version;
}
