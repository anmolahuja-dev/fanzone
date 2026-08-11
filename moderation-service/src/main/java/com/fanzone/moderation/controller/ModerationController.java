package com.fanzone.moderation.controller;

import com.fanzone.moderation.dto.ModerationRequest;
import com.fanzone.moderation.dto.ToxicityResult;
import com.fanzone.moderation.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for content moderation.
 * Called by post-service before publishing content.
 */
@RestController
@RequestMapping("/api/v1/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ToxicityResult> analyze(@Valid @RequestBody ModerationRequest request) {
        ToxicityResult result = moderationService.analyzeContent(request.content());
        return ResponseEntity.ok(result);
    }
}
