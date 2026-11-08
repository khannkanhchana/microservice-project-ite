package kh.edu.istad.iam.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** IAM user profile, OIDC standard claims, and Spring Security account state. */
public final class User implements UserDetails {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9][0-9]{1,14}$");
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Z]{2}$");

    private final Long id;
    private final UUID uuid;
    private final String username;
    private String passwordHash;
    private final String givenName;
    private final String familyName;
    private final String middleName;
    private final String nickname;
    private final String gender;
    private final LocalDate birthdate;
    private final String locale;
    private final String profileUrl;
    private final String pictureUrl;
    private final String websiteUrl;
    private final String email;
    private final boolean emailVerified;
    private final String phoneNumber;
    private final boolean phoneNumberVerified;
    private final String zoneinfo;
    private final String addressStreetAddress;
    private final String addressLocality;
    private final String addressRegion;
    private final String addressPostalCode;
    private final String addressCountry;
    private boolean enabled;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private int failedLoginAttempts;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private final Instant createdAt;
    private final String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private boolean deleted;
    private Instant deletedAt;
    private String deletedBy;
    private long version;
    private final Set<String> authorities;

    private User(Builder builder) {
        this.id = builder.id;
        this.uuid = Objects.requireNonNull(builder.uuid, "uuid is required");
        this.username = required(builder.username, "username", 50);
        this.passwordHash = optional(builder.passwordHash, 255, "passwordHash");
        this.givenName = required(builder.givenName, "givenName", 100);
        this.familyName = required(builder.familyName, "familyName", 100);
        this.middleName = optional(builder.middleName, 100, "middleName");
        this.nickname = optional(builder.nickname, 100, "nickname");
        this.gender = optional(builder.gender, 20, "gender");
        this.birthdate = builder.birthdate;
        this.locale = required(builder.locale, "locale", 10);
        this.profileUrl = optional(builder.profileUrl, 500, "profileUrl");
        this.pictureUrl = optional(builder.pictureUrl, 500, "pictureUrl");
        this.websiteUrl = optional(builder.websiteUrl, 500, "websiteUrl");
        this.email = required(builder.email, "email", 255).toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(this.email).matches()) throw new IllegalArgumentException("email must be a valid address");
        this.emailVerified = builder.emailVerified;
        this.phoneNumber = optional(builder.phoneNumber, 20, "phoneNumber");
        if (this.phoneNumber != null && !PHONE_PATTERN.matcher(this.phoneNumber).matches()) {
            throw new IllegalArgumentException("phoneNumber must use E.164 format");
        }
        this.phoneNumberVerified = builder.phoneNumberVerified;
        this.zoneinfo = optional(builder.zoneinfo, 50, "zoneinfo");
        this.addressStreetAddress = optional(builder.addressStreetAddress, 255, "addressStreetAddress");
        this.addressLocality = optional(builder.addressLocality, 100, "addressLocality");
        this.addressRegion = optional(builder.addressRegion, 100, "addressRegion");
        this.addressPostalCode = optional(builder.addressPostalCode, 20, "addressPostalCode");
        String normalizedCountry = optional(builder.addressCountry, 2, "addressCountry");
        if (normalizedCountry != null) {
            normalizedCountry = normalizedCountry.toUpperCase(Locale.ROOT);
            if (!COUNTRY_PATTERN.matcher(normalizedCountry).matches()) {
                throw new IllegalArgumentException("addressCountry must be an ISO 3166-1 alpha-2 code");
            }
        }
        this.addressCountry = normalizedCountry;
        this.enabled = builder.enabled;
        this.accountNonExpired = builder.accountNonExpired;
        this.accountNonLocked = builder.accountNonLocked;
        this.credentialsNonExpired = builder.credentialsNonExpired;
        if (builder.failedLoginAttempts < 0) throw new IllegalArgumentException("failedLoginAttempts cannot be negative");
        this.failedLoginAttempts = builder.failedLoginAttempts;
        this.lastLoginAt = builder.lastLoginAt;
        this.passwordChangedAt = builder.passwordChangedAt;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
        this.createdBy = required(builder.createdBy, "createdBy", 255);
        this.updatedAt = builder.updatedAt;
        this.updatedBy = optional(builder.updatedBy, 255, "updatedBy");
        if (updatedAt != null && updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt cannot be before createdAt");
        }
        this.deleted = builder.deleted;
        this.deletedAt = builder.deletedAt;
        this.deletedBy = optional(builder.deletedBy, 255, "deletedBy");
        if (deleted && deletedAt == null) throw new IllegalArgumentException("deletedAt is required for a deleted user");
        if (deletedAt != null && deletedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("deletedAt cannot be before createdAt");
        }
        if (!deleted && (deletedAt != null || deletedBy != null)) throw new IllegalArgumentException("Deletion audit data requires deleted=true");
        if (builder.version < 0) throw new IllegalArgumentException("version cannot be negative");
        this.version = builder.version;
        this.authorities = Set.copyOf(builder.authorities);
    }

    /** OIDC subject claim: the stable public user identifier. */
    public String getSubject() { return uuid.toString(); }

    /** OIDC name claim. */
    public String getName() { return middleName == null ? givenName + " " + familyName : givenName + " " + middleName + " " + familyName; }

    /** OIDC preferred_username claim. */
    public String getPreferredUsername() { return username; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() { return passwordHash; }

    public String getPasswordHash() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return accountNonExpired && !deleted; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }

    @Override
    public boolean isEnabled() { return enabled && !deleted; }

    public void recordFailedLogin(Instant at) {
        recordFailedLogin(at, "system");
    }

    public void recordFailedLogin(Instant at, String actor) {
        requireUsableTimestamp(at);
        if (failedLoginAttempts < Integer.MAX_VALUE) failedLoginAttempts++;
        markUpdated(at, actor);
    }

    public void recordSuccessfulLogin(Instant at, String actor) {
        requireUsableTimestamp(at);
        lastLoginAt = at;
        failedLoginAttempts = 0;
        markUpdated(at, actor);
    }

    public void changePassword(String encodedPassword, Instant changedAt, String actor) {
        String validatedHash = required(encodedPassword, "passwordHash", 255);
        requireUsableTimestamp(changedAt);
        passwordHash = validatedHash;
        passwordChangedAt = changedAt;
        credentialsNonExpired = true;
        markUpdated(changedAt, actor);
    }

    public void softDelete(Instant at, String actor) {
        if (deleted) throw new IllegalStateException("User has already been soft-deleted");
        requireUsableTimestamp(at);
        deleted = true;
        deletedAt = at;
        deletedBy = required(actor, "deletedBy", 255);
        markUpdated(at, actor);
    }

    public void restore(Instant at, String actor) {
        if (!deleted) throw new IllegalStateException("User is not deleted");
        requireUsableTimestamp(at);
        deleted = false;
        deletedAt = null;
        deletedBy = null;
        markUpdated(at, actor);
    }

    public void setEnabled(boolean enabled, Instant at, String actor) {
        requireUsableTimestamp(at);
        this.enabled = enabled;
        markUpdated(at, actor);
    }

    public void setAccountNonLocked(boolean accountNonLocked, Instant at, String actor) {
        requireUsableTimestamp(at);
        this.accountNonLocked = accountNonLocked;
        markUpdated(at, actor);
    }

    private void markUpdated(Instant at, String actor) {
        updatedAt = at;
        updatedBy = required(actor, "updatedBy", 255);
        version++;
    }

    private void requireUsableTimestamp(Instant at) {
        Objects.requireNonNull(at, "timestamp is required");
        if (at.isBefore(createdAt)) throw new IllegalArgumentException("timestamp cannot be before createdAt");
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        return normalized;
    }

    private static String optional(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        return normalized;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getGivenName() { return givenName; }
    public String getFamilyName() { return familyName; }
    public String getMiddleName() { return middleName; }
    public String getNickname() { return nickname; }
    public String getGender() { return gender; }
    public LocalDate getBirthdate() { return birthdate; }
    public String getLocale() { return locale; }
    public String getProfileUrl() { return profileUrl; }
    public String getPictureUrl() { return pictureUrl; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isPhoneNumberVerified() { return phoneNumberVerified; }
    public String getZoneinfo() { return zoneinfo; }
    public String getAddressStreetAddress() { return addressStreetAddress; }
    public String getAddressLocality() { return addressLocality; }
    public String getAddressRegion() { return addressRegion; }
    public String getAddressPostalCode() { return addressPostalCode; }
    public String getAddressCountry() { return addressCountry; }
    public boolean isEnabledFlag() { return enabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public boolean isDeleted() { return deleted; }
    public Instant getDeletedAt() { return deletedAt; }
    public String getDeletedBy() { return deletedBy; }
    public long getVersion() { return version; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private UUID uuid = UUID.randomUUID();
        private String username;
        private String passwordHash;
        private String givenName;
        private String familyName;
        private String middleName;
        private String nickname;
        private String gender;
        private LocalDate birthdate;
        private String locale;
        private String profileUrl;
        private String pictureUrl;
        private String websiteUrl;
        private String email;
        private boolean emailVerified;
        private String phoneNumber;
        private boolean phoneNumberVerified;
        private String zoneinfo;
        private String addressStreetAddress;
        private String addressLocality;
        private String addressRegion;
        private String addressPostalCode;
        private String addressCountry;
        private boolean enabled = true;
        private boolean accountNonExpired = true;
        private boolean accountNonLocked = true;
        private boolean credentialsNonExpired = true;
        private int failedLoginAttempts;
        private Instant lastLoginAt;
        private Instant passwordChangedAt;
        private Instant createdAt = Instant.now();
        private String createdBy = "system";
        private Instant updatedAt;
        private String updatedBy;
        private boolean deleted;
        private Instant deletedAt;
        private String deletedBy;
        private long version;
        private Set<String> authorities = Set.of();

        public Builder id(Long value) { id = value; return this; }
        public Builder uuid(UUID value) { uuid = value; return this; }
        public Builder username(String value) { username = value; return this; }
        public Builder passwordHash(String value) { passwordHash = value; return this; }
        public Builder givenName(String value) { givenName = value; return this; }
        public Builder familyName(String value) { familyName = value; return this; }
        public Builder middleName(String value) { middleName = value; return this; }
        public Builder nickname(String value) { nickname = value; return this; }
        public Builder gender(String value) { gender = value; return this; }
        public Builder birthdate(LocalDate value) { birthdate = value; return this; }
        public Builder locale(String value) { locale = value; return this; }
        public Builder profileUrl(String value) { profileUrl = value; return this; }
        public Builder pictureUrl(String value) { pictureUrl = value; return this; }
        public Builder websiteUrl(String value) { websiteUrl = value; return this; }
        public Builder email(String value) { email = value; return this; }
        public Builder emailVerified(boolean value) { emailVerified = value; return this; }
        public Builder phoneNumber(String value) { phoneNumber = value; return this; }
        public Builder phoneNumberVerified(boolean value) { phoneNumberVerified = value; return this; }
        public Builder zoneinfo(String value) { zoneinfo = value; return this; }
        public Builder addressStreetAddress(String value) { addressStreetAddress = value; return this; }
        public Builder addressLocality(String value) { addressLocality = value; return this; }
        public Builder addressRegion(String value) { addressRegion = value; return this; }
        public Builder addressPostalCode(String value) { addressPostalCode = value; return this; }
        public Builder addressCountry(String value) { addressCountry = value; return this; }
        public Builder enabled(boolean value) { enabled = value; return this; }
        public Builder accountNonExpired(boolean value) { accountNonExpired = value; return this; }
        public Builder accountNonLocked(boolean value) { accountNonLocked = value; return this; }
        public Builder credentialsNonExpired(boolean value) { credentialsNonExpired = value; return this; }
        public Builder failedLoginAttempts(int value) { failedLoginAttempts = value; return this; }
        public Builder lastLoginAt(Instant value) { lastLoginAt = value; return this; }
        public Builder passwordChangedAt(Instant value) { passwordChangedAt = value; return this; }
        public Builder createdAt(Instant value) { createdAt = value; return this; }
        public Builder createdBy(String value) { createdBy = value; return this; }
        public Builder updatedAt(Instant value) { updatedAt = value; return this; }
        public Builder updatedBy(String value) { updatedBy = value; return this; }
        public Builder deleted(boolean value) { deleted = value; return this; }
        public Builder deletedAt(Instant value) { deletedAt = value; return this; }
        public Builder deletedBy(String value) { deletedBy = value; return this; }
        public Builder version(long value) { version = value; return this; }
        public Builder authorities(Set<String> value) { authorities = Set.copyOf(value); return this; }

        public User build() { return new User(this); }
    }
}
