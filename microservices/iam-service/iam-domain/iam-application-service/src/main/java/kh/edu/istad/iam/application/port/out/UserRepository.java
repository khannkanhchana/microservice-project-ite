package kh.edu.istad.iam.application.port.out;

import kh.edu.istad.iam.application.query.UserFilterQuery;
import kh.edu.istad.iam.application.result.PageResult;
import kh.edu.istad.iam.application.result.UserSummary;
import kh.edu.istad.iam.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByUuid(UUID userUuid);
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    PageResult<UserSummary> search(UserFilterQuery query);
}
