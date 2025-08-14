package com.project.demo.logic.entity.auth;

import com.project.demo.service.AuthJwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthJwtService authJwtService;

    @Autowired
    public OAuth2LoginSuccessHandler(AuthJwtService authJwtService) {
        this.authJwtService = authJwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        String token = authJwtService.generateToken(authentication);

        String redirectUrl = "http://localhost:4200/app/dashboard?token=" + token;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        super.onAuthenticationSuccess(request, response, authentication);
    }
}