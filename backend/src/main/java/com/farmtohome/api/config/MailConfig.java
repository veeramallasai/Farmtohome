package com.farmtohome.api.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

  static {
    // Prefer IPv4 stack to prevent IPv6 DNS/routing connection timeouts in Docker cloud containers
    System.setProperty("java.net.preferIPv4Stack", "true");
  }

  @Bean
  @Primary
  public JavaMailSender javaMailSender(
      @Value("${spring.mail.host:${SPRING_MAIL_HOST:${MAIL_HOST:smtp.gmail.com}}}") String host,
      @Value("${spring.mail.port:${SPRING_MAIL_PORT:${MAIL_PORT:465}}}") String portStr,
      @Value("${spring.mail.username:${SPRING_MAIL_USERNAME:${MAIL_USERNAME:${APP_MAIL_FROM:${MAIL_FROM:veeramallasaipichaiah456@gmail.com}}}}}") String username,
      @Value("${spring.mail.password:${SPRING_MAIL_PASSWORD:${MAIL_PASSWORD:hinnvjmxxziliiim}}}") String password) {

    int port = 465;
    try {
      if (portStr != null && !portStr.trim().isEmpty()) {
        port = Integer.parseInt(portStr.trim());
      }
    } catch (NumberFormatException e) {
      System.err.println("[MAIL-CONFIG-WARN] Invalid mail port '" + portStr + "', falling back to 465");
      port = 465;
    }

    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    String effectiveHost = (host != null && !host.isBlank()) ? host.trim() : "smtp.gmail.com";
    mailSender.setHost(effectiveHost);
    mailSender.setPort(port);
    mailSender.setUsername(username != null ? username.trim() : "");
    mailSender.setPassword(password != null ? password.trim() : "");
    mailSender.setProtocol("smtp");
    mailSender.setDefaultEncoding("UTF-8");

    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.ssl.trust", "*");
    props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
    props.put("mail.smtp.connectiontimeout", "15000");
    props.put("mail.smtp.timeout", "15000");
    props.put("mail.smtp.writetimeout", "15000");

    String securityMode;
    if (port == 465) {
      securityMode = "SSL/TLS (Implicit - Port 465)";
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.starttls.enable", "false");
      props.put("mail.smtp.starttls.required", "false");
      props.put("mail.smtp.socketFactory.port", "465");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.socketFactory.fallback", "false");
    } else {
      securityMode = "STARTTLS (Explicit - Port " + port + ")";
      props.put("mail.smtp.ssl.enable", "false");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
      props.remove("mail.smtp.socketFactory.port");
      props.remove("mail.smtp.socketFactory.class");
      props.remove("mail.smtp.socketFactory.fallback");
    }

    System.out.println("=================================================");
    System.out.println("[MAIL-CONFIG] Initialized JavaMailSender");
    System.out.println("[MAIL-CONFIG] Host: " + mailSender.getHost());
    System.out.println("[MAIL-CONFIG] Port: " + mailSender.getPort());
    System.out.println("[MAIL-CONFIG] Username: " + mask(mailSender.getUsername()));
    System.out.println("[MAIL-CONFIG] Password Configured: " + (password != null && !password.isBlank() ? "YES (Length=" + password.trim().length() + ")" : "NO"));
    System.out.println("[MAIL-CONFIG] Security Mode: " + securityMode);
    System.out.println("=================================================");

    return mailSender;
  }

  public static String mask(String input) {
    if (input == null || input.isBlank()) return "<empty>";
    int at = input.indexOf('@');
    if (at <= 1) return "***";
    String local = input.substring(0, at);
    String domain = input.substring(at);
    if (local.length() <= 2) {
      return local.charAt(0) + "***" + domain;
    }
    return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
  }
}

