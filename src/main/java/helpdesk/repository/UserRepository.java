package helpdesk.repository;

import helpdesk.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    List<User> findAllByRole(String role);
    User findByVerificationToken(String token);
}
