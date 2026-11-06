package kh.edu.istad.iam.feature.user;

import kh.edu.istad.iam.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {


}
