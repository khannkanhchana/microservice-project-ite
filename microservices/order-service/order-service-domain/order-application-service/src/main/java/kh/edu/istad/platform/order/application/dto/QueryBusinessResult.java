package kh.edu.istad.platform.order.application.dto;

import java.util.Map;
import java.util.List;

// Matches the Business API's domain response, including its nested value objects.
public record QueryBusinessResult(
        Map<String, Object> id,
        Map<String, Object> name,
        Map<String, Object> contact,
        boolean active,
        List<Map<String, Object>> products) {
}
