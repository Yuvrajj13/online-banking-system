package com.beko.DemoBank_v1.controllers;

import com.beko.DemoBank_v1.helpers.HTML;
import com.beko.DemoBank_v1.helpers.Token;
import com.beko.DemoBank_v1.mailMessenger.MailMessenger;
import com.beko.DemoBank_v1.models.User;
import com.beko.DemoBank_v1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.util.*;

@RestController
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user, BindingResult bindingResult, @RequestParam("confirm_password") String confirmPassword) {


        String firstName = user.getFirst_name();
        String lastName = user.getLast_name();
        String email = user.getEmail();
        String password = user.getPassword();

        if(bindingResult.hasErrors() && confirmPassword.isEmpty()){
            List<String> errorMessages = new ArrayList<>();
            for(FieldError error : bindingResult.getFieldErrors()){
                errorMessages.add(error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errorMessages);
        }

        //TODO: CHECK FOR PASSWORD MATCH:
        if(!password.equals(confirmPassword))
            return ResponseEntity.badRequest().body("Şifreler uyuşmuyor.");

        //TODO: CHECK IF EMAIL ALREADY EXISTS:
        if (userRepository.getUserEmail(email) != null) {
            return ResponseEntity.badRequest().body("This email address is already registered. Please sign in instead.");
        }

        //TODO: GET TOKEN STRING:
        String token = Token.generateToken();

        //TODO: GENERATE RANDOM CODE:
        Random rand = new Random();
        int bound = 123;
        int code = bound * rand.nextInt(bound);

        //TODO: GET EMAIL HTML BODY
        String emailBody = HTML.htmlEmailTemplate(token, Integer.toString(code));
        //TODO: HASH PASSWORD:
        String hashed_password = BCrypt.hashpw(password, BCrypt.gensalt());

        //TODO: REGISTER USER:
        userRepository.registerUser(firstName, lastName, email, hashed_password, token, Integer.toString(code));

        //TODO: SEND EMAIL NOTIFICATION (Optional: fail-safe if no local mail server is running)
        try {
            MailMessenger.htmlEmailMessenger("user@beko.com", email, "Verify Account", emailBody);
        } catch (Exception e) {
            System.err.println("Note: Email server not active. Proceeding with registration without mail delivery: " + e.getMessage());
        }

        //TODO: RETURN REGISTRATION SUCCESS
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful. You can now log in!");
        response.put("user", user);
        return ResponseEntity.ok(response);
    }
}
