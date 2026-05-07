package com.example.notificationservice.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SendGridWebhookEvent {
    private String email;
    private Long timestamp;
    private String event;
    private String reason;
    private String status;
    private String type;

    @JsonProperty("sg_event_id")
    private String sgEventId;

    @JsonProperty("sg_message_id")
    private String sgMessageId;

    @JsonProperty("smtp-id")
    private String smtpId;

    // Custom args that we send with emails
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("notification_id")
    private String notificationId;
}
