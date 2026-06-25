package com.queueify.campaignservice.authentication.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
//    private JavaMailSender mailSender;
//
//    @Value("${email.hostname}")
//    private String username;
//
//    public boolean sendEmail(String email, String subject, String body){
//
//        SimpleMailMessage mailMessage = new SimpleMailMessage();
//        mailMessage.setSubject(subject);
//        mailMessage.setFrom(username);
//        mailMessage.setTo(email);
//        mailMessage.setText(body);
//
//        mailSender.send(mailMessage);
//
//        System.out.println("Mail sent successfully to " + username);
//        return true;
//    }
//
}
