package com.meditrack.Repository;

import com.meditrack.enums.Role;
import com.meditrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public class UserRepository  extends JpaRepository<User,Long>{
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    long countByRoleAndActiveTrue(Role role);
    List<User> findByRole(Role role);
    List<User> findByRoleIn(List<Role> roles);
}
