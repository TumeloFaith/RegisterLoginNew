package com.example.RegisterLogIn.Web;

import com.example.RegisterLogIn.Exception.UserAlreadyExistsException;
import com.example.RegisterLogIn.Model.User;
import com.example.RegisterLogIn.Service.UserService;
import com.example.RegisterLogIn.Web.DTO.UserRegistrationDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/useraccount")
public class UserAccountController {

    private final UserService userService;

    public UserAccountController(UserService userService) {
        this.userService = userService;
    }

    // Display login page
    @GetMapping("/login")
    public String login() {
        return "login"; // Return the login view
    }

    // Handle login
    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            Model model) {
        try {
            User user = userService.findByEmail(email);
            if (user == null || !user.isEnabled()) {
                throw new IllegalArgumentException("User account is not verified or does not exist.");
            }

            userService.authenticateUser(email, password); // Continue with authentication logic
            return "redirect:/home"; // Redirect to home page
        } catch (IllegalArgumentException | UsernameNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "login"; // Return login view with error
        }
    }

    // Display registration form
    @GetMapping("/registration")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDTO());
        return "registration"; // Return registration view
    }

    // Handle user registration
    @PostMapping("/registration")
    public String registerUserAccount(@ModelAttribute("user") @Valid UserRegistrationDTO registrationDTO,
                                      BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "registration"; // Return registration view with validation errors
        }

        if (userService.emailExists(registrationDTO.getEmail())) {
            model.addAttribute("errorMessage", "Email already registered! Please use a different email.");
            return "registration"; // Return registration view with error message
        }

        try {
            userService.save(registrationDTO); // Save user
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "registration"; // Return registration view with error message
        }

        redirectAttributes.addFlashAttribute("message", "Registration successful! Please check your email to verify your account.");
        return "redirect:/login"; // Redirect to login after successful registration
    }

    // Handle email verification
    @GetMapping("/registration/verify")
    public String verifyUser(@RequestParam("token") String token, Model model) {
        boolean isVerified = userService.verifyUser(token);

        if (isVerified) {
            model.addAttribute("message", "Email verified successfully! You can now log in.");
            return "login"; // Redirect to login page
        } else {
            model.addAttribute("errorMessage", "Invalid verification token. Please try again.");
            return "error"; // Return error page
        }
    }

    // Display home page
    @GetMapping("/home")
    public String home() {
        return "index"; // Return home or index view
    }
}
