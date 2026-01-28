package com.bhavani.resumeforge.controller;

import com.bhavani.resumeforge.service.TemplatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
@Slf4j
public class TemplatesController {

    private final TemplatesService templatesService;

    @GetMapping
    public ResponseEntity<?> getTemplates(Authentication authentication) {
        //Step 1: call the service method
        Map<String, Object> response = templatesService.getTemplates(authentication.getPrincipal());

        //step 2: return the response
        return ResponseEntity.ok(response);
    }
}
