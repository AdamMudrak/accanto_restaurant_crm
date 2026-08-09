package com.example.restaurant.service.email.dtos;

public record EmailTexts(
            String subject,
            String title,
            String greeting,
            String description,
            String button,
            String securityNote
){}
