package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.entity.RepeatOrderEntity;
import com.sorted.rest.services.order.services.OrderService;
import com.sorted.rest.services.order.services.RepeatOrderService;

import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Repeat Order Services", description = "Manage Repeat order related services.")
public class RepeatOrderController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(OrderController.class);

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private RepeatOrderService repeatOrderService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private ClientService clientService;

	@ApiOperation(value = "Create Repeat Order", nickname = "createRepeatOrder")
	@PostMapping("/orders/repeat")
	public ResponseEntity<RepeatOrderResponse> createRepeatOrder(@Valid @RequestBody() RepeatOrderCreateRequest repeatOrderRequest) {
		_LOGGER.info(String.format("Creating repeat order: Request %s", repeatOrderRequest));
		UUID customerId = orderService.getUserId();
		if (repeatOrderRequest.getPreferences().getQuantity() == null || repeatOrderRequest.getPreferences().getQuantity() <= 0) {
			_LOGGER.error("Quantity should be greater than 0");
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Quantity should be greater than 0", "quantity"));
		}
		if (CollectionUtils.isEmpty(repeatOrderRequest.getPreferences().getDays())) {
			_LOGGER.error("Days should not be empty");
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Days should not be empty", "days"));
		}

		RepeatOrderEntity existingRepeatOrder = repeatOrderService.findByCustomerIdAndSkuCode(customerId, repeatOrderRequest.getSkuCode());
		if (existingRepeatOrder != null) {
			_LOGGER.error("Repeat Order for sku already exists");
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Repeat Order for sku already exists", "repeatOrder"));
		}
		RepeatOrderEntity repeatOrder = repeatOrderService.createRepeatOrder(repeatOrderRequest, customerId);
		if (repeatOrder == null) {
			return ResponseEntity.badRequest().build();
		}
		RepeatOrderResponse response = new RepeatOrderResponse();
		mapper.mapSrcToDest(repeatOrder, response);
		setDeliverTomorrowDialog(response, repeatOrder.getCustomerId());
		setRepeatOrderSetDialog(response);
		return ResponseEntity.ok(response);
	}

	private void setRepeatOrderSetDialog(RepeatOrderResponse response) {
		RepeatOrderResponse.DeliverTomorrowDialog dialog = new RepeatOrderResponse.DeliverTomorrowDialog();
		dialog.setTitle("Your repeat order\n is set!");
		String repeatOrderDialogImage = ParamsUtils.getParam("REPEAT_ORDER_DIALOG_IMAGE",
				"https://d69ugcdrlg41w.cloudfront.net/public/eec8275d-8dc2-4b7e-807e-8ac2e1f7412b.png");
		dialog.setImage(repeatOrderDialogImage);
		DateFormat dateFormat = new SimpleDateFormat("EEEE, d", Locale.ENGLISH);
		String formattedDate = dateFormat.format(response.getNextDeliveryDate());
		String dayWithSuffix = DeliveryDateUtils.addDayOfMonthSuffix(formattedDate);
		dialog.setHeading(String.format("<b>Your first delivery will be\n on %s</b>", dayWithSuffix));

		dialog.setMessage("Please ensure you have\n sufficient wallet balance for\n uninterrupted delivery!");
		dialog.setCta("Cool!");
		response.setRepeatOrderSetDialog(dialog);
	}

	public void setDeliverTomorrowDialog(RepeatOrderResponse response, UUID customerId) {
		LocalDate tomorrow = DateUtils.getLocalDateIST().plusDays(1);
		if (response.getPreferences().getDays().contains(tomorrow.getDayOfWeek())) {
			String storeId = repeatOrderService.getUserStore(customerId);
			if (storeId == null) {
				return;
			}
			StoreInventoryResponse inventoryResponse = clientService.getStoreInventory(storeId, response.getSkuCode(), null);
			StoreInventoryResponse.StoreProductInventory product = inventoryResponse.getInventory().get(0);
			RepeatOrderResponse.DeliverTomorrowDialog dialog = new RepeatOrderResponse.DeliverTomorrowDialog();
			dialog.setTitle(String.format("Tomorrow's %s?", product.getProductName()));
			String tomorrowDialogImage = ParamsUtils.getParam("TOMORROW_DIALOG_IMAGE",
					"https://d69ugcdrlg41w.cloudfront.net/public/5329e629-d734-43fa-836f-49104cb06d01.png");
			dialog.setImage(tomorrowDialogImage);
			dialog.setHeading(String.format("<b>If you want %s\n delivered tomorrow</b>", product.getProductName()));
			dialog.setMessage("Please add it to your cart");
			dialog.setCta("Sure");
			response.setDeliverTomorrowDialog(dialog);
		}
	}

	@ApiOperation(value = "Create Back Office Order", nickname = "createBackofficeOrder")
	@PutMapping("/orders/repeat/{id}")
	public ResponseEntity<RepeatOrderResponse> updateRepeatOrder(@Valid @RequestBody() RepeatOrderUpdateRequest repeatOrderRequest, @PathVariable Long id) {
		_LOGGER.info(String.format("Updating repeat order with id %d: Request %s", id, repeatOrderRequest));
		RepeatOrderEntity repeatOrder = repeatOrderService.findRecordById(id);
		if (repeatOrder == null) {
			_LOGGER.error("Repeat Order not found");
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Repeat Order not found", "repeatOrder"));
		}
		UUID userId = orderService.getUserId();
		if (!repeatOrder.getCustomerId().equals(userId)) {
			_LOGGER.error("Not authorized to update this repeat order");
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "You are not authorized to update this repeat order", "repeatOrder"));
		}
		repeatOrder = repeatOrderService.updateRepeatOrder(repeatOrder, repeatOrderRequest);
		RepeatOrderResponse response = new RepeatOrderResponse();
		mapper.mapSrcToDest(repeatOrder, response);
		if (repeatOrder.getStatus().equals(OrderConstants.RepeatOrderStatus.ACTIVE) && repeatOrder.getActive() == 1) {
			setRepeatOrderSetDialog(response);
			setDeliverTomorrowDialog(response, repeatOrder.getCustomerId());
		}
		if (repeatOrder.getActive() == 0) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/orders/repeat")
	public ResponseEntity<List<RepeatOrderResponse>> getCustomerRepeatOrders() {
		UUID customerId = orderService.getUserId();
		List<RepeatOrderEntity> repeatOrders = repeatOrderService.getCustomerActiveRepeatOrders(customerId);
		if (CollectionUtils.isEmpty(repeatOrders)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(
				repeatOrders.stream().map(repeatOrder -> mapper.mapSrcToDest(repeatOrder, RepeatOrderResponse.newInstance())).collect(Collectors.toList()));
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
