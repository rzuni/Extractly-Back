package com.project.demo.rest.passwordReset;

import com.project.demo.service.PasswordResetService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request){
        String email = request.get("email");
        if(email == null || email.trim().isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("message", "El correo electrónico es requerido."));
        }
        //Se devuelve un mensaje generico para no revelar si el correo exite o no.
        passwordResetService.createPasswordResetTokenForUser(email);
        return ResponseEntity.ok(Map.of("message", "Si tu correo electrónico esta registrado correctamente, recibiras un enlace para restablecer tu contraseña."));
    }

    @PostMapping("/reset-Password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request){

        if(request.getToken() == null || request.getToken().trim().isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("message", "El token es requerido."));
        }
        if(request.getNewPassword() == null || request.getNewPassword().trim().isEmpty() || request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("message", "La nueva contraseña y la confirmación son requeridas."));
        }
        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            return ResponseEntity.badRequest().body(Map.of("message", "Las contraseñas no coinciden."));
        }

        try{
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    public static class ResetPasswordRequest{
        private String token;
        private String newPassword;
        private String confirmPassword;

        public ResetPasswordRequest(){}

        public ResetPasswordRequest(String token, String newPassword, String confirmPassword){
            this.token = token;
            this.newPassword = newPassword;
            this.confirmPassword = confirmPassword;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
