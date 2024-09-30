package com.example.RegisterLogIn.Web;

import com.example.RegisterLogIn.Model.User;
import com.example.RegisterLogIn.Service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Controller
public class MainController {

    private final UserService userService;

    // Constructor for dependency injection
    public MainController(UserService userService) {
        this.userService = userService; // Assign injected UserService
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Return the login view
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            Model model) {
        try {
            User user = userService.findByEmail(email); // Fetch user by email

            // Check if user is null or not enabled
            if (user == null || !user.isEnabled()) {
                throw new IllegalArgumentException("User account is not verified or does not exist.");
            }

            userService.authenticateUser(email, password); // Continue with authentication logic
            return "redirect:/home"; // Redirect to the home page or desired page
        } catch (IllegalArgumentException | UsernameNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "login"; // Return to login page with error message
        }
    }

    @GetMapping("/")
    public String home() {
        return "index"; // Return the home view
    }
}
