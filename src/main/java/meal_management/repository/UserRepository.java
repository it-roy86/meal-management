package meal_management.repository;

import meal_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 아이디로 사용자 조회 (Spring Security에서 사용)
    Optional<User> findByUsername(String username);

    // 아이디 중복 확인
    boolean existsByUsername(String username);
}