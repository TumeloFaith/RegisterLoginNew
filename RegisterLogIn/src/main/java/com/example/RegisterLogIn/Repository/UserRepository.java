package com.example.RegisterLogIn.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.RegisterLogIn.Model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email); // Change return type to Optional<User>
    Optional<User> findByVerificationToken(String token); // Change return type to Optional<User>
}
