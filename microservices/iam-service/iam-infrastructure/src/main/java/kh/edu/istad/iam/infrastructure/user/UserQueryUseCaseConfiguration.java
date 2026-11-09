package kh.edu.istad.iam.infrastructure.user;

import kh.edu.istad.iam.application.port.out.PermissionRepository;
import kh.edu.istad.iam.application.port.out.RolePermissionAssignmentRepository;
import kh.edu.istad.iam.application.port.out.RoleRepository;
import kh.edu.istad.iam.application.port.out.UserPermissionAssignmentRepository;
import kh.edu.istad.iam.application.port.out.UserRepository;
import kh.edu.istad.iam.application.port.out.UserRoleAssignmentRepository;
import kh.edu.istad.iam.application.service.UpdateUserProfileService;
import kh.edu.istad.iam.application.usecase.GetUserAuthoritiesUseCase;
import kh.edu.istad.iam.application.usecase.GetUserUseCase;
import kh.edu.istad.iam.application.service.GetUserAuthoritiesService;
import kh.edu.istad.iam.application.service.GetUserService;
import kh.edu.istad.iam.application.usecase.UpdateUserProfileUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root wiring for application query services and persistence output ports. */
@Configuration(proxyBeanMethods = false)
public class UserQueryUseCaseConfiguration {
    @Bean
    GetUserUseCase getUserUseCase(UserRepository users) {
        return new GetUserService(users);
    }

    @Bean
    GetUserAuthoritiesUseCase getUserAuthoritiesUseCase(
            UserRepository users, UserRoleAssignmentRepository userRoles,
            UserPermissionAssignmentRepository userPermissions,
            RolePermissionAssignmentRepository rolePermissions,
            RoleRepository roles, PermissionRepository permissions) {
        return new GetUserAuthoritiesService(users, userRoles, userPermissions, rolePermissions, roles, permissions);
    }

    @Bean
    UpdateUserProfileUseCase updateUserProfileUseCase(
            UserRepository userRepository
    ) {
        return new UpdateUserProfileService(userRepository);
    }
}
