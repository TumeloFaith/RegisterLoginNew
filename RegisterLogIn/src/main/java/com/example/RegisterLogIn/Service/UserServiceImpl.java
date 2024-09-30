package com.example.RegisterLogIn.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.RegisterLogIn.Exception.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.RegisterLogIn.Model.Role;
import com.example.RegisterLogIn.Model.User;
import com.example.RegisterLogIn.Repository.UserRepository;
import com.example.RegisterLogIn.Web.DTO.UserRegistrationDTO;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Override
    public User save(UserRegistrationDTO registrationDto) {
        return createUser(registrationDto);
    }

    @Override
    public User saveUserWithVerificationToken(UserRegistrationDTO registrationDto) {
        return createUser(registrationDto);
    }

    private User createUser(UserRegistrationDTO registrationDto) {
        // Check if the user already exists
        Optional<User> existingUser = userRepository.findByEmail(registrationDto.getEmail());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + registrationDto.getEmail() + " already exists");
        }

        // Create a new user and set properties
        User user = new User(
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getEmail(),
                passwordEncoder.encode(registrationDto.getPassword()),
                Arrays.asList(new Role("ROLE_USER"))
        );

        // Set enabled to false until the email is verified
        user.setEnabled(false);

        // Generate a unique verification token
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);

        // Save the user to the repository
        userRepository.save(user);

        // Construct the verification URL
        String verificationUrl = "http://localhost:8080/registration/verifyUser?token=" + token;

        // Send the verification email
        sendVerificationEmail(user.getEmail(), verificationUrl);

        logger.info("Verification email sent to {}", user.getEmail());
        return user; // Return the saved user object
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl) {
        if (verificationUrl == null || verificationUrl.contains("token=null")) {
            logger.error("Attempting to send verification email with a null token.");
            return; // Prevent sending an email with a null token
        }
        String subject = "Email Verification";
        String message = "Thank you for registering! Please verify your email and log in by clicking on the link: " + verificationUrl;

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        try {
            mailSender.send(mailMessage);
            logger.info("Email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", email, e.getMessage());
        }
    }

    @Override
    public boolean verifyUser(String token) {
        Optional<User> optionalUser = userRepository.findByVerificationToken(token);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setEnabled(true); // Enable the user after verification
            user.setVerificationToken(null); // Clear the token after verification
            userRepository.save(user);
            logger.info("User {} verified successfully.", user.getEmail());
            return true; // User verified successfully
        }
        logger.warn("Verification failed: Token invalid or user not found for token {}", token);
        return false; // Token invalid or user not found
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> optionalUser = userRepository.findByEmail(username);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("Invalid email or password.");
        }

        User user = optionalUser.get();

        // Check if the user is enabled before allowing login
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User is not verified. Please check your email for verification.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                mapRolesToAuthorities(user.getRoles())
        );
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public void authenticateUser(String username, String rawPassword) {
        UserDetails userDetails = loadUserByUsername(username);

        // Check if the raw password matches the encoded password
        if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            throw new UsernameNotFoundException("Invalid email or password.");
        }

        // Authentication successful logic can go here
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }
}
