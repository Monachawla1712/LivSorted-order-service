package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCountBean {

	private UUID userId;

	private Long count;
}
