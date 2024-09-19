package com.example.RegisterLogIn.Service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.example.RegisterLogIn.Model.User;
import com.example.RegisterLogIn.Web.DTO.UserRegistrationDTO;

public interface UserService extends UserDetailsService{
    User save(UserRegistrationDTO registrationDto);
}
