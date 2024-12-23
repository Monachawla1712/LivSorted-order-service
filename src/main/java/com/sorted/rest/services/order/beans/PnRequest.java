package com.sorted.rest.services.order.beans;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PnRequest {
    private String userId;

    private String templateName;

    private Map<String, String> fillers;
}
