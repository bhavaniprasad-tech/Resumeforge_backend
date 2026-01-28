package com.bhavani.resumeforge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendHtmlEmail(String to, String subject, String htmlContent) {

        log.info("Inside the EmailService: sendHtmlEmail() {}, {}, {}", to, subject, htmlContent);

        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        String body = """
        {
          "sender": {
            "name": "%s",
            "email": "%s"
          },
          "to": [
            {
              "email": "%s"
            }
          ],
          "subject": "%s",
          "htmlContent": "%s"
        }
        """.formatted(
                senderName,
                senderEmail,
                to,
                subject,
                htmlContent.replace("\"", "\\\"")
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            log.info("Brevo email sent successfully: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Brevo email sending failed", e);
            throw new RuntimeException("Failed to send email");
        }
    }

    public void sentEmailWithAttachment(
            String to,
            String subject,
            String body,
            byte[] attachment,
            String filename
    ) {

        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        String encodedAttachment =
                java.util.Base64.getEncoder().encodeToString(attachment);

        // Attachment object
        Map<String, Object> attachmentMap = new HashMap<>();
        attachmentMap.put("content", encodedAttachment);
        attachmentMap.put("name", filename);
        attachmentMap.put("type", "application/pdf");

        // Request body
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of(
                "name", senderName,
                "email", senderEmail
        ));
        payload.put("to", List.of(Map.of("email", to)));
        payload.put("subject", subject);
        payload.put("htmlContent", body);
        payload.put("attachment", List.of(attachmentMap));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            log.info("Email with attachment sent successfully");
        } catch (Exception e) {
            log.error("Failed to send email with attachment", e);
            throw new RuntimeException("Email sending failed");
        }
    }


}
