package com.sorted.rest.services.order.services;

import com.sorted.rest.services.order.beans.PnRequest;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.entity.OrderEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class NotificationService {
    public PnRequest getOrderDeliveredPnRequest(OrderEntity order) {
        return PnRequest.builder()
                .userId(order.getCustomerId().toString())
                .templateName(OrderConstants.ORDER_DELIVERED_PN_TEMPLATE_NAME)
                .fillers(Collections.emptyMap())
                .build();
    }
}
