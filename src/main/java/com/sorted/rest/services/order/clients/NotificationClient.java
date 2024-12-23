package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "notificationClient", url = "${client.notification.url}", configuration = { FeignCustomConfiguration.class })
public interface NotificationClient {

	@PostMapping(value = "/notification/pn/send")
	void sendPushNotifications(@RequestBody List<PnRequest> request);

	@PostMapping(value = "/notification/email/send")
	void sendEmail(@RequestBody NotificationServiceEmailRequest emailRequest);

	@PostMapping(value = "/notification/whatsapp/send")
	SendNotificationsResponse sendWhatsappMessages(@RequestBody WhatsappSendMsgRequest request);

	@PostMapping(value = "/notification/clevertap/profile-update")
	void sendClevertapEvent(ClevertapEventRequest clevertapProfileUpdateRequest);
}
