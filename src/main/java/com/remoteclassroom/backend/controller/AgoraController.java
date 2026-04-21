package com.remoteclassroom.backend.controller;

import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agora")
public class AgoraController {

    @Value("${AGORA_APP_ID}")
    private String appId;

    @Value("${AGORA_APP_CERTIFICATE}")
    private String appCertificate;

    @GetMapping("/token")
    public Map<String, String> generateToken(
            @RequestParam String channelName,
            @RequestParam(defaultValue = "0") int uid
    ) {
        if (appId == null || appId.isEmpty() || appCertificate == null || appCertificate.isEmpty()) {
            return Map.of("error", "Agora credentials not configured in backend");
        }

        int expirationTimeInSeconds = 3600;
        int timestamp = (int) (System.currentTimeMillis() / 1000 + expirationTimeInSeconds);

        RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();
        String token = tokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                channelName,
                uid,
                Role.Role_Publisher,
                timestamp
        );

        return Map.of("token", token);
    }
}
