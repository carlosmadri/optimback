package com.airbus.optim.controller;

import com.airbus.optim.dto.UserPropertiesDTO;
import com.airbus.optim.service.UserService;
import com.airbus.optim.utils.TokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("user")
public class UserController {

    @Value("${app.extractUsernameFromToken}")
    private boolean EXTRACT_USERNAME_FROM_TOKEN;

    @Autowired
    public UserService userService;

    @Autowired
    public TokenValidator tokenValidator;

    @GetMapping("/user-properties")
    public ResponseEntity<UserPropertiesDTO> getUserProperties(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "userSelected", required = false) String userSelected) {

        try {
            var username = EXTRACT_USERNAME_FROM_TOKEN 
                ? Optional.ofNullable(token)
                    .map(t -> t.replace("Bearer ", "").trim())
                    .map(tokenValidator::extractUsername)
                    .filter(u -> !u.isEmpty())
                    .orElse(null)
                : Optional.ofNullable(userSelected)
                    .filter(u -> !u.isEmpty())
                    .orElse(null);

            if (username == null) {
                return ResponseEntity.status(EXTRACT_USERNAME_FROM_TOKEN ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST).body(null);
            }

            var userProperties = Optional.ofNullable(userService.getUserProperties(username));
            
            return userProperties.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
