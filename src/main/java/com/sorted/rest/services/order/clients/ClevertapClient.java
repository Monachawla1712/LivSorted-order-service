package com.sorted.rest.services.order.clients;

import com.sorted.rest.services.order.beans.ClevertapEventRequest;
import com.sorted.rest.services.order.beans.ClevertapEventResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(value = "clevertapClient", url = "${client.clevertap.url}")
public interface ClevertapClient {

	@PostMapping(value = "/1/upload")
	ClevertapEventResponse sendEvent(@RequestHeader Map<String, Object> headers, @RequestBody ClevertapEventRequest request) ;

}