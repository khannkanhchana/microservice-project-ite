package kh.edu.istad.common.webflux.dto;

import java.time.LocalDateTime;

public record ErrorApiResponse(
        String message,
        int status,
        LocalDateTime timestamp
) {
}