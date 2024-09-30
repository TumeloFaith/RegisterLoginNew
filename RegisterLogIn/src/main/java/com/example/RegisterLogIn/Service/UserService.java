package com.example.RegisterLogIn.Service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.example.RegisterLogIn.Model.User;
import com.example.RegisterLogIn.Web.DTO.UserRegistrationDTO;

public interface UserService extends UserDetailsService {

    User save(UserRegistrationDTO registrationDto);

    void authenticateUser(String username, String rawPassword);

    boolean verifyUser(String token);

    boolean emailExists(String email);

    User findByEmail(String email); // Method to find a user by email

    /**
     * Saves the user with a verification token.
     *
     * @param registrationDto the user registration data transfer object
     */
    default User saveUserWithVerificationToken(UserRegistrationDTO registrationDto) {
        return null;
    }

    /**
     * Sends a verification email to the user.
     *
     * @param email the email address of the user
     * @param verificationUrl the verification URL to be sent in the email
     */
    void sendVerificationEmail(String email, String verificationUrl);
}
