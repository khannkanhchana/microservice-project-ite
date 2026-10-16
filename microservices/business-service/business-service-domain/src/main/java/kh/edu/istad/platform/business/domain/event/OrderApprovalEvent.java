package kh.edu.istad.platform.business.domain.event;

import kh.edu.istad.platform.business.domain.entity.OrderApproval;
import kh.edu.istad.platform.business.domain.valueobject.BusinessId;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderApprovalEvent(OrderApproval orderApproval, BusinessId businessId, List<String> failureMessages, ZonedDateTime createdAt) {
    public OrderApprovalEvent { failureMessages = List.copyOf(failureMessages); }
}
