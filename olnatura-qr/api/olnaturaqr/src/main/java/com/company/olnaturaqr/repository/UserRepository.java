package com.company.olnaturaqr.repository;

import com.company.olnaturaqr.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByUsernameIgnoreCase(String username);
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByUsernameIgnoreCase(String username);
  boolean existsByEmailIgnoreCase(String email);

  List<User> findTop50ByEnabledFalseOrderByCreatedAtDesc();

  List<User> findAllByOrderByCreatedAtDesc();

  long countByEnabledFalse();

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from User u where u.enabled = false")
  int deleteByEnabledFalse();
}
