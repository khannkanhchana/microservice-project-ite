package kh.edu.istad.iam.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    @Column(name = "given_name", nullable = false, length = 100)
    private String givenName;
    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;
    @Column(name = "middle_name", length = 100)
    private String middleName;
    @Column(length = 100)
    private String nickname;
    @Column(length = 20)
    private String gender;
    private LocalDate birthdate;
    @Column(nullable = false, length = 10)
    private String locale;
    @Column(name = "profile_url", length = 500)
    private String profileUrl;
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;
    @Column(name = "website_url", length = 500)
    private String websiteUrl;
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;
    @Column(name = "phone_number_verified", nullable = false)
    private boolean phoneNumberVerified;
    @Column(length = 50)
    private String zoneinfo;
    @Column(name = "address_street_address", length = 255)
    private String addressStreetAddress;
    @Column(name = "address_locality", length = 100)
    private String addressLocality;
    @Column(name = "address_region", length = 100)
    private String addressRegion;
    @Column(name = "address_postal_code", length = 20)
    private String addressPostalCode;
    @Column(name = "address_country", length = 2)
    private String addressCountry;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired;
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked;
    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired;
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;
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
