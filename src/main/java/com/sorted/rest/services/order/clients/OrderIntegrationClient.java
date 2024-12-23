package com.sorted.rest.services.order.clients;

import com.sorted.rest.services.order.beans.SendOrderToLithosResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.LithosOrderBean;

@FeignClient(value = "orderIntegration", url = "${client.util.url}", configuration = { FeignCustomConfiguration.class })
public interface OrderIntegrationClient {

	@GetMapping(value = "/util/internal/lithos/create")
	SendOrderToLithosResponse sendOrderToLithos(@RequestBody LithosOrderBean order);

	@GetMapping(value = "/util/internal/lithos/payment-update")
	SendOrderToLithosResponse sendPaymentUpdateToLithos(@RequestBody LithosOrderBean order);
}