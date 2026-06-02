package microservices.appointmentservice.repositories;

import microservices.appointmentservice.entities.user.User;
import microservices.appointmentservice.utils.Constants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findAllByRole(Constants.RoleEnum role);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (:name IS NULL OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    List<User> findAllByNameAndRole(@Param("name") String name, @Param("role") Constants.RoleEnum role);
}
