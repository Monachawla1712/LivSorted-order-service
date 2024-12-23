package com.sorted.rest.services.order.controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.sorted.rest.services.order.beans.PpdOrderItemBean;
import com.sorted.rest.services.order.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.OrderItemResponseBean;
import com.sorted.rest.services.order.entity.OrderItemEntity;
import com.sorted.rest.services.order.services.OrderItemService;

import io.swagger.annotations.ApiOperation;

/**
 * Created by Mohit.
 */
@RestController
@RequestMapping("")
public class OrderItemController implements BaseController {

	@Autowired
	private OrderItemService orderItemService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	/**
	 * Find one.
	 *
	 */
	@ApiOperation(value = "Get one Order Item with provided id.", nickname = "findOneOrderItem")
	@GetMapping("/orders/items/{id}")
	public ResponseEntity<OrderItemResponseBean> findOne(@PathVariable UUID id) {
		OrderItemEntity entity = orderItemService.findById(id);
		if (entity == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}
		return ResponseEntity.ok(getMapper().mapSrcToDest(entity, new OrderItemResponseBean()));
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

}
