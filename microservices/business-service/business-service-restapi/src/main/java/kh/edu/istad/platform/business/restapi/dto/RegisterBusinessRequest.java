package kh.edu.istad.platform.business.restapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterBusinessRequest(
        @NotBlank
        @Size(max=200)
        String legalName,

        @NotBlank
        @Size(max=200)
        String displayName,

        @NotBlank
        @Email
        @Size(max=254)
        String email,
        @NotBlank
        @Size(max=30)
        String phoneNumber) {}
