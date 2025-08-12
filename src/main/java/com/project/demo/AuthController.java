package com.project.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RestController
public class AuthController {

    @GetMapping("/")
    public String publicPage() {
        return "Página pública";
    }

    @GetMapping("/loginSuccess")
    public String getLoginInfo(@AuthenticationPrincipal OAuth2User user) {
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");

        return "Inicio de sesión exitoso. ¡Hola, " + name + "!";
    }

    @GetMapping("/loginFailure")
    public String loginFailure() {
        return "Error en el inicio de sesión.";
    }
}
