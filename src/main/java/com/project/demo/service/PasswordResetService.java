package com.project.demo.service;

import com.project.demo.logic.entity.passwordResetToken.PasswordResetToken;
import com.project.demo.logic.entity.passwordResetToken.PasswordResetTokenRepository;
import com.project.demo.logic.entity.user.User;
import com.project.demo.logic.entity.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontUrl;

    private static final int TOKEN_EXPIRATION_TIME = 30;

    public void createPasswordResetTokenForUser(String email){
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if(user != null){
            String token = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_TIME);

            //Elimina tokens anteriores para el mismo usuario para evitar múltiples enlaces válidos
            tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(expiryDate);
            tokenRepository.save(resetToken);

            sendPasswordResetEmail(user, token);
        }
    }

    private void sendPasswordResetEmail(User user, String token){

        String resetUrl = frontUrl + "/resetPassword?token=" + token;
        String subject = "Restablecimiento de contraseña";
        String body = "Hola " + user.getName() + ",\n\n"
                    + "Si haz solicitado restablecer tu constraseña. Haz click en el siguiente enlace para continua: \n"
                    + resetUrl + "\n\n"
                    + "Este enlace expirará en 30 minutos. Si no solicitaste esto, ignora este correo.\n\n"
                    + "Atentamente,\nTu equipo";

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject(subject);
        email.setText(body);
        mailSender.send(email);
    }

    public boolean validatePasswordResetToken(String token){

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElse(null);

        return resetToken != null && resetToken.getExpiryDate().isBefore(LocalDateTime.now());
    }

    public void resetPassword(String token, String newPassword){
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

        if(resetToken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Token Expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword)); //Hashea la nueva contraseña
        userRepository.save(user);

        tokenRepository.delete(resetToken);//Invalida el token después de usarlo
    }
}
