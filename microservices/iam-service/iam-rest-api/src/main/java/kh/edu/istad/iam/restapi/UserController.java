package kh.edu.istad.iam.restapi;

import kh.edu.istad.iam.application.admin.SystemSettingsUseCase;
import jakarta.validation.Valid;
import kh.edu.istad.iam.application.account.AccountActivityType;
import kh.edu.istad.iam.application.account.AccountSelfServiceUseCase;
import kh.edu.istad.iam.application.command.*;
import kh.edu.istad.iam.domain.AssignmentEffect;
import kh.edu.istad.iam.application.query.SortDirection;
import kh.edu.istad.iam.application.query.UserFilterQuery;
import kh.edu.istad.iam.application.result.*;
import kh.edu.istad.iam.application.usecase.*;
import kh.edu.istad.iam.restapi.dto.UserRequests;
import kh.edu.istad.iam.restapi.dto.UserResponses;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api")
public class UserController {

    /** System Configuration: registration and password policies (Admin > Settings). */
    @org.springframework.beans.factory.annotation.Autowired
    private ObjectProvider<SystemSettingsUseCase> policies;

    private void requireValidPassword(String password) {
        SystemSettingsUseCase settings = policies.getIfAvailable();
        if (settings != null) settings.requireValidPassword(password);
    }
    private final ObjectProvider<RegisterUserUseCase> register;
    private final ObjectProvider<CreateUserUseCase> create;
    private final ObjectProvider<GetUserUseCase> get;
    private final ObjectProvider<ListUsersUseCase> list;
    private final ObjectProvider<UpdateUserProfileUseCase> update;
    private final ObjectProvider<ChangePasswordUseCase> changePassword;
    private final ObjectProvider<ResetUserPasswordUseCase> resetPassword;
    private final ObjectProvider<EnableUserUseCase> enable;
    private final ObjectProvider<DisableUserUseCase> disable;
    private final ObjectProvider<LockUserUseCase> lock;
    private final ObjectProvider<UnlockUserUseCase> unlock;
    private final ObjectProvider<DeleteUserUseCase> delete;
    private final ObjectProvider<AssignRolesToUserUseCase> assignRoles;
    private final ObjectProvider<RevokeRoleFromUserUseCase> revokeRole;
    private final ObjectProvider<GrantPermissionsToUserUseCase> grantPermissions;
    private final ObjectProvider<RevokePermissionFromUserUseCase> revokePermission;
    private final ObjectProvider<GetUserAuthoritiesUseCase> authorities;
    private final ObjectProvider<ListUserIdentitiesUseCase> identities;
    private final ObjectProvider<UnlinkIdentityUseCase> unlinkIdentity;
    private final ObjectProvider<AccountSelfServiceUseCase> account;

    public UserController(ObjectProvider<RegisterUserUseCase> register, ObjectProvider<CreateUserUseCase> create,
                          ObjectProvider<GetUserUseCase> get, ObjectProvider<ListUsersUseCase> list,
                          ObjectProvider<UpdateUserProfileUseCase> update,
                          ObjectProvider<ChangePasswordUseCase> changePassword,
                          ObjectProvider<ResetUserPasswordUseCase> resetPassword,
                          ObjectProvider<EnableUserUseCase> enable, ObjectProvider<DisableUserUseCase> disable,
                          ObjectProvider<LockUserUseCase> lock, ObjectProvider<UnlockUserUseCase> unlock,
                          ObjectProvider<DeleteUserUseCase> delete,
                          ObjectProvider<AssignRolesToUserUseCase> assignRoles,
                          ObjectProvider<RevokeRoleFromUserUseCase> revokeRole,
                          ObjectProvider<GrantPermissionsToUserUseCase> grantPermissions,
                          ObjectProvider<RevokePermissionFromUserUseCase> revokePermission,
                          ObjectProvider<GetUserAuthoritiesUseCase> authorities,
                          ObjectProvider<ListUserIdentitiesUseCase> identities,
                          ObjectProvider<UnlinkIdentityUseCase> unlinkIdentity,
                          ObjectProvider<AccountSelfServiceUseCase> account) {
        this.register = register; this.create = create; this.get = get; this.list = list; this.update = update;
        this.changePassword = changePassword; this.resetPassword = resetPassword; this.enable = enable;
        this.disable = disable; this.lock = lock; this.unlock = unlock; this.delete = delete;
        this.assignRoles = assignRoles; this.revokeRole = revokeRole; this.grantPermissions = grantPermissions;
        this.revokePermission = revokePermission; this.authorities = authorities; this.identities = identities;
        this.unlinkIdentity = unlinkIdentity;
        this.account = account;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResult register(@Valid @RequestBody UserRequests.Registration body) {
        SystemSettingsUseCase settings = policies.getIfAvailable();
        if (settings != null && !settings.get().authentication().allowSelfRegistration()) {
            throw new IllegalArgumentException("Self-registration is disabled. Contact an administrator.");
        }
        requireMatchingPasswords(body.password(), body.confirmPassword());
        requireValidPassword(body.password());
        return required(register, "RegisterUserUseCase").execute(new RegisterUserCommand(
                body.username(), body.password(), body.givenName(), body.familyName(), body.locale(), body.email(),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponses.MeResponse me(Authentication auth) { return meResponse(subject(auth)); }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponses.MeResponse updateMe(@Valid @RequestBody UserRequests.Profile body, Authentication auth) {
        UUID uuid = subject(auth);
        AccountSelfServiceUseCase accountService = required(account, "AccountSelfServiceUseCase");
        accountService.updateContact(uuid, body.username(), body.email(), body.phoneNumber(), actor(auth));
        List<String> profileFields = changedProfileFields(body);
        if (!profileFields.isEmpty()) {
            update(uuid, body, actor(auth), null);
            accountService.recordActivity(uuid, AccountActivityType.PROFILE_UPDATED,
                    String.join(", ", profileFields), actor(auth));
        }
        return meResponse(uuid);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void changeMyPassword(@Valid @RequestBody UserRequests.PasswordChange body, Authentication auth,
                                 @RequestAttribute(name = "iam.currentSessionId", required = false) UUID currentSession) {
        requireMatchingPasswords(body.newPassword(), body.confirmPassword());
        requireValidPassword(body.newPassword());
        required(changePassword, "ChangePasswordUseCase").execute(
                new ChangePasswordCommand(subject(auth), body.currentPassword(), body.newPassword(), actor(auth)));
        if (Boolean.TRUE.equals(body.signOutOtherSessions())) {
            required(account, "AccountSelfServiceUseCase").revokeOtherSessions(subject(auth), currentSession, actor(auth));
        }
    }

    @GetMapping("/me/identities")
    @PreAuthorize("isAuthenticated()")
    public List<UserResponses.IdentityResponse> myIdentities(Authentication auth) {
        return identityResponses(subject(auth));
    }

    @DeleteMapping("/me/identities/{identityUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void unlinkMyIdentity(@PathVariable UUID identityUuid, Authentication auth) {
        UUID uuid = subject(auth);
        String provider = identityResponses(uuid).stream().filter(identity -> identity.uuid().equals(identityUuid))
                .map(UserResponses.IdentityResponse::provider).findFirst().orElse(null);
        required(unlinkIdentity, "UnlinkIdentityUseCase").execute(uuid, identityUuid, Instant.now(), actor(auth));
        required(account, "AccountSelfServiceUseCase").recordActivity(uuid, AccountActivityType.IDENTITY_UNLINKED,
                provider, actor(auth));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:read')")
    public PageResult<UserSummary> users(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(defaultValue = "createdAt,desc") String sort,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(required = false) Boolean enabled) {
        String[] sortParts = sort.split(",", 2);
        SortDirection direction = sortParts.length == 1 ? SortDirection.ASC : SortDirection.valueOf(sortParts[1].toUpperCase());
        return required(list, "ListUsersUseCase").execute(new UserFilterQuery(search, enabled, false,
                page, size, sortParts[0], direction));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('user:create')")
    public UserResult createUser(@Valid @RequestBody UserRequests.CreateUser body, Authentication auth) {
        requireValidPassword(body.password());
        UserRequests.Address address = body.address();
        UserResult result = required(create, "CreateUserUseCase").execute(new CreateUserCommand(
                body.username(), body.password(), body.givenName(), body.familyName(), body.locale(), body.email(),
                body.middleName(), body.nickname(), body.gender(), body.birthdate(), body.profileUrl(), body.pictureUrl(),
                body.websiteUrl(), body.phoneNumber(), body.zoneinfo(),
                address == null ? null : address.streetAddress(), address == null ? null : address.locality(),
                address == null ? null : address.region(), address == null ? null : address.postalCode(),
                address == null ? null : address.country(), actor(auth)));
        if (!body.roleUuids().isEmpty()) {
            if (body.roleUuids().stream().distinct().count() != body.roleUuids().size()) {
                throw new IllegalArgumentException("roleUuids must not contain duplicates.");
            }
            required(assignRoles, "AssignRolesToUserUseCase").execute(new AssignRolesToUserCommand(
                    result.uuid(), body.roleUuids(), actor(auth), null, "Initial roles assigned during user creation"));
        }
        return result;
    }

    @GetMapping("/users/{uuid}")
    @PreAuthorize("hasAuthority('user:read')")
    public UserResponses.MeResponse user(@PathVariable UUID uuid) { return meResponse(uuid); }

    @PatchMapping("/users/{uuid}")
    @PreAuthorize("hasAuthority('user:update')")
    public UserResult updateUser(@PathVariable UUID uuid, @Valid @RequestBody UserRequests.Profile body, Authentication auth) {
        return update(uuid, body, actor(auth));
    }

    @DeleteMapping("/users/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:delete')")
    public void deleteUser(@PathVariable UUID uuid, Authentication auth) {
        required(delete, "DeleteUserUseCase").execute(uuid, Instant.now(), actor(auth));
    }

    @PostMapping("/users/{uuid}/enable") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void enable(@PathVariable UUID uuid, Authentication auth) { required(enable, "EnableUserUseCase").execute(uuid, Instant.now(), actor(auth)); }

    @PostMapping("/users/{uuid}/disable") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void disable(@PathVariable UUID uuid, Authentication auth) { required(disable, "DisableUserUseCase").execute(uuid, Instant.now(), actor(auth)); }

    @PostMapping("/users/{uuid}/lock") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void lock(@PathVariable UUID uuid, Authentication auth) { required(lock, "LockUserUseCase").execute(uuid, Instant.now(), actor(auth)); }

    @PostMapping("/users/{uuid}/unlock") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void unlock(@PathVariable UUID uuid, Authentication auth) { required(unlock, "UnlockUserUseCase").execute(uuid, Instant.now(), actor(auth)); }

    @PutMapping("/users/{uuid}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void resetPassword(@PathVariable UUID uuid, @Valid @RequestBody UserRequests.PasswordReset body, Authentication auth) {
        requireValidPassword(body.newPassword());
        required(resetPassword, "ResetUserPasswordUseCase").execute(new ResetUserPasswordCommand(uuid, body.newPassword(), actor(auth)));
    }

    @GetMapping("/users/{uuid}/authorities")
    @PreAuthorize("hasAuthority('user:read')")
    public UserResponses.UserAuthoritiesResponse authorities(@PathVariable UUID uuid) {
        UserAuthoritiesResult result = required(authorities, "GetUserAuthoritiesUseCase").execute(uuid);
        return new UserResponses.UserAuthoritiesResponse(result.userUuid(),
                result.roles().stream().map(role -> new UserResponses.RoleAuthority(
                        role.uuid(), role.name(), role.expiresAt())).toList(),
                result.directPermissions().stream().map(permission -> new UserResponses.DirectPermission(
                        permission.uuid(), permission.name(), permission.effect().name(), permission.expiresAt())).toList(),
                result.effectivePermissions());
    }

    @PostMapping("/users/{uuid}/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('user:assign')")
    public List<RoleAssignmentResult> assignRoles(@PathVariable UUID uuid, @Valid @RequestBody UserRequests.RoleAssignments body, Authentication auth) {
        return required(assignRoles, "AssignRolesToUserUseCase").execute(new AssignRolesToUserCommand(
                uuid, body.roleUuids(), actor(auth), body.expiresAt(), body.reason()));
    }

    @DeleteMapping("/users/{uuid}/roles/{roleUuid}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:assign')")
    public void revokeRole(@PathVariable UUID uuid, @PathVariable UUID roleUuid,
                           @RequestParam(required = false) String reason, Authentication auth) {
        required(revokeRole, "RevokeRoleFromUserUseCase").execute(uuid, roleUuid, reason, actor(auth), Instant.now());
    }

    @PostMapping("/users/{uuid}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('user:assign')")
    public List<PermissionGrantResult> grantPermissions(@PathVariable UUID uuid,
            @Valid @RequestBody UserRequests.PermissionGrants body, Authentication auth) {
        AssignmentEffect effect = body.effect() == null ? AssignmentEffect.GRANT : AssignmentEffect.valueOf(body.effect().toUpperCase());
        return required(grantPermissions, "GrantPermissionsToUserUseCase").execute(new GrantPermissionsToUserCommand(
                uuid, body.permissionUuids(), effect, actor(auth), body.expiresAt(), body.reason()));
    }

    @DeleteMapping("/users/{uuid}/permissions/{permissionUuid}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:assign')")
    public void revokePermission(@PathVariable UUID uuid, @PathVariable UUID permissionUuid,
                                 @RequestParam(required = false) String reason, Authentication auth) {
        required(revokePermission, "RevokePermissionFromUserUseCase").execute(uuid, permissionUuid, reason, actor(auth), Instant.now());
    }

    @GetMapping("/users/{uuid}/identities")
    @PreAuthorize("hasAuthority('user:read')")
    public List<UserResponses.IdentityResponse> userIdentities(@PathVariable UUID uuid) {
        return identityResponses(uuid);
    }

    @DeleteMapping("/users/{uuid}/identities/{identityUuid}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('user:update')")
    public void unlinkIdentity(@PathVariable UUID uuid, @PathVariable UUID identityUuid, Authentication auth) {
        required(unlinkIdentity, "UnlinkIdentityUseCase").execute(uuid, identityUuid, Instant.now(), actor(auth));
    }

    private UserResult update(UUID uuid, UserRequests.Profile body, String actor) {
        return update(uuid, body, actor, body.phoneNumber());
    }

    private UserResult update(UUID uuid, UserRequests.Profile body, String actor, String phoneNumber) {
        return required(update, "UpdateUserProfileUseCase").execute(new UpdateUserProfileCommand(uuid,
                body.givenName(), body.familyName(), body.middleName(), body.nickname(), body.gender(), body.birthdate(),
                body.locale(), body.profileUrl(), body.pictureUrl(), body.websiteUrl(), phoneNumber, body.zoneinfo(),
                body.addressStreetAddress(), body.addressLocality(), body.addressRegion(), body.addressPostalCode(),
                body.addressCountry(), actor));
    }

    private UserResponses.MeResponse meResponse(UUID uuid) {
        UserResult user = required(get, "GetUserUseCase").execute(uuid);
        UserAuthoritiesResult userAuthorities = required(authorities, "GetUserAuthoritiesUseCase").execute(uuid);
        List<UserResponses.Role> roles = userAuthorities.roles().stream()
                .map(role -> new UserResponses.Role(role.uuid(), role.name())).toList();
        String name = java.util.stream.Stream.of(user.givenName(), user.familyName())
                .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining(" "));
        UserResponses.Address address = new UserResponses.Address(user.addressStreetAddress(), user.addressLocality(),
                user.addressRegion(), user.addressPostalCode(), user.addressCountry());
        return new UserResponses.MeResponse(user.uuid(), user.username(), user.email(), user.emailVerified(), name,
                user.givenName(), user.familyName(), user.middleName(), user.nickname(), user.gender(), user.birthdate(),
                user.locale(), user.zoneinfo(), user.pictureUrl(), user.websiteUrl(), user.phoneNumber(),
                user.phoneNumberVerified(), address, user.enabled(),
                required(account, "AccountSelfServiceUseCase").hasPassword(uuid), roles,
                userAuthorities.effectivePermissions(), user.lastLoginAt(), user.passwordChangedAt(),
                user.createdAt(), user.updatedAt());
    }

    /** Names of the non-contact profile fields present in a PATCH body (contact fields are handled separately). */
    private static List<String> changedProfileFields(UserRequests.Profile body) {
        java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();
        fields.put("givenName", body.givenName()); fields.put("familyName", body.familyName());
        fields.put("middleName", body.middleName()); fields.put("nickname", body.nickname());
        fields.put("gender", body.gender()); fields.put("birthdate", body.birthdate());
        fields.put("locale", body.locale()); fields.put("profileUrl", body.profileUrl());
        fields.put("pictureUrl", body.pictureUrl()); fields.put("websiteUrl", body.websiteUrl());
        fields.put("zoneinfo", body.zoneinfo()); fields.put("addressStreetAddress", body.addressStreetAddress());
        fields.put("addressLocality", body.addressLocality()); fields.put("addressRegion", body.addressRegion());
        fields.put("addressPostalCode", body.addressPostalCode()); fields.put("addressCountry", body.addressCountry());
        return fields.entrySet().stream().filter(entry -> entry.getValue() != null)
                .map(java.util.Map.Entry::getKey).toList();
    }

    private UserResponses.UserResponse userResponse(UUID uuid) {
        UserResult user = required(get, "GetUserUseCase").execute(uuid);
        UserAuthoritiesResult userAuthorities = required(authorities, "GetUserAuthoritiesUseCase").execute(uuid);
        List<UserResponses.Role> roles = userAuthorities.roles().stream()
                .map(role -> new UserResponses.Role(role.uuid(), role.name())).toList();
        String name = java.util.stream.Stream.of(user.givenName(), user.familyName())
                .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining(" "));
        return new UserResponses.UserResponse(user.uuid(), user.username(), user.email(), user.emailVerified(),
                name, user.locale(), user.enabled(), roles, user.createdAt(), user.updatedAt());
    }

    private List<UserResponses.IdentityResponse> identityResponses(UUID userUuid) {
        return required(identities, "ListUserIdentitiesUseCase").execute(userUuid).stream()
                .map(identity -> new UserResponses.IdentityResponse(identity.uuid(), identity.provider(),
                        identity.email(), identity.displayName(), identity.username(), identity.pictureUrl(),
                        identity.linkedAt(), identity.lastLoginAt())).toList();
    }

    private static UUID subject(Authentication auth) {
        try { return UUID.fromString(auth.getName()); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject must be the user's UUID."); }
    }

    private static String actor(Authentication auth) { return auth == null ? "anonymous" : auth.getName(); }

    private static void requireMatchingPasswords(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("confirmPassword must match password.");
        }
    }

    private static <T> T required(ObjectProvider<T> provider, String useCase) {
        T bean = provider.getIfAvailable();
        if (bean == null) throw new UseCaseUnavailableException(useCase);
        return bean;
    }
}
