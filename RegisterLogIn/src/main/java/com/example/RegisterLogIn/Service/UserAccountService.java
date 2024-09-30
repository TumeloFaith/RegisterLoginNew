package com.example.RegisterLogIn.Service;

import com.example.RegisterLogIn.Exception.InvalidCredentialsException; // Custom exception for invalid credentials
import com.example.RegisterLogIn.Exception.UserAlreadyExistsException;
import com.example.RegisterLogIn.Model.Role;
import com.example.RegisterLogIn.Model.User;
import com.example.RegisterLogIn.Repository.UserRepository;
import com.example.RegisterLogIn.Web.DTO.UserRegistrationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Autowired
    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    /**
     * Register a new user.
     *
     * @param registrationDto the DTO containing user registration data
     * @return the newly created User object
     */
    public User registerUser(UserRegistrationDTO registrationDto) {
        Optional<User> existingUser = userRepository.findByEmail(registrationDto.getEmail());

        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + registrationDto.getEmail() + " already exists");
        }

        // Create a new user object
        User user = new User(
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getEmail(),
                passwordEncoder.encode(registrationDto.getPassword()),
                Arrays.asList(new Role("ROLE_USER"))
        );

        user.setEnabled(false); // Set enabled to false by default

        // Generate a unique verification token
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);

        // Save the user to the repository
        userRepository.save(user);

        // Send verification email
        sendVerificationEmail(user.getEmail(), token);

        return user;
    }

    /**
     * Send a verification email.
     *
     * @param to    the recipient's email address
     * @param token the verification token to include in the email
     */
    private void sendVerificationEmail(String to, String token) {
        String subject = "Email Verification";
        String verificationUrl = "https://yourapp.com/registration/verify?token=" + token;
        String message = "Please click the following link to verify your email: " + verificationUrl;

        // Send the email
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }

    /**
     * Verify a user account using a verification token.
     *
     * @param token the token sent to the user's email
     * @return the verified User object
     */
    public User verifyUser(String token) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);

        if (!userOptional.isPresent()) {
            throw new IllegalArgumentException("Invalid verification token");
        }

        User user = userOptional.get();
        user.setEnabled(true); // Set user as enabled
        user.setVerificationToken(null); // Clear the verification token
        userRepository.save(user);

        return user;
    }

    /**
     * Authenticate a user using email and password.
     *
     * @param email    the user's email
     * @param password the user's password
     * @return the authenticated User object
     */
    public User authenticateUser(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (!optionalUser.isPresent()) {
            throw new InvalidCredentialsException("Invalid email or password."); // Throw a custom exception
        }

        User user = optionalUser.get();

        // Verify the password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password."); // Throw a custom exception
        }

        // Optionally, you can check if the user is enabled
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is not verified.");
        }

        return user; // Return the authenticated user
    }

    // You can add other related methods for managing user accounts
}
