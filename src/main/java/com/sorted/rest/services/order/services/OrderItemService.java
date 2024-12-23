package com.sorted.rest.services.order.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.entity.OrderItemEntity;
import com.sorted.rest.services.order.repository.OrderItemRepository;

/**
 * 
 * Created by mohit on 20.8.22.
 */
@Service
public class OrderItemService implements BaseService<OrderItemEntity> {

	@Autowired
	private OrderItemRepository orderItemRepository;

	public OrderItemEntity findById(UUID id) {
		Optional<OrderItemEntity> result = orderItemRepository.findById(id);
		if(result.isPresent()) {
			return result.get();
		}
		return null;
	}

	public OrderItemEntity save(OrderItemEntity entity) {
		OrderItemEntity result = orderItemRepository.save(entity);
		return result;
	}

	@Override
	public Class<OrderItemEntity> getEntity() {
		return OrderItemEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return orderItemRepository;
	}

}