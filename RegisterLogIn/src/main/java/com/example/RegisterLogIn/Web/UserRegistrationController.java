package com.example.RegisterLogIn.Web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.RegisterLogIn.Exception.UserAlreadyExistsException;
import com.example.RegisterLogIn.Service.UserService;
import com.example.RegisterLogIn.Web.DTO.UserRegistrationDTO;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/registration")
public class UserRegistrationController {

    private final UserService userService;
    private final Logger logger = LoggerFactory.getLogger(UserRegistrationController.class);

    public UserRegistrationController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("user")
    public UserRegistrationDTO userRegistrationDto() {
        return new UserRegistrationDTO();
    }

    @GetMapping
    public String showRegistrationForm() {
        return "registration"; // Return the registration view
    }

    @PostMapping
    public String registerUserAccount(@ModelAttribute("user") @Valid UserRegistrationDTO registrationDTO,
                                      BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            logger.warn("Registration error: {}", result.getAllErrors());
            return "registration"; // Render the registration view with error messages
        }

        // Check if the email already exists
        if (userService.emailExists(registrationDTO.getEmail())) {
            model.addAttribute("errorMessage", "Email already registered! Please use a different email.");
            logger.info("Email already registered: {}", registrationDTO.getEmail());
            return "registration"; // Return to registration view with error message
        }

        try {
            // Save the user with a verification token
            userService.saveUserWithVerificationToken(registrationDTO);

            // Send verification email with the token
            String verificationUrl = "http://localhost:8080/registration/verifyUser?token=" + registrationDTO.getVerificationToken();
            userService.sendVerificationEmail(registrationDTO.getEmail(), verificationUrl);

            // Add a success message to notify the user to check their email
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please check your email to verify your account.");
            return "redirect:/login";

        } catch (UserAlreadyExistsException e) {
            logger.error("User already exists: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "registration"; // Return to registration view with error message
        }
    }

    @GetMapping("/verifyUser")
    public String verifyUser(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        // Verify the token using the userService
        boolean isVerified = userService.verifyUser(token);

        if (isVerified) {
            // Set success message and redirect to login page
            redirectAttributes.addFlashAttribute("message", "Email verified successfully! You can now log in.");
            return "redirect:/login"; // Redirect to login page
        } else {
            // Set error message and redirect to an error page
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid verification token. Please try again.");
            return "redirect:/error"; // Redirect to an error page or a suitable view
        }
    }
}
