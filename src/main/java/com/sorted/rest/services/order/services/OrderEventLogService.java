package com.sorted.rest.services.order.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.entity.OrderEventLogEntity;
import com.sorted.rest.services.order.repository.OrderEventLogRepository;

/**
 * Created by Abhishek on 20.8.22.
 */
@Service
public class OrderEventLogService implements BaseService<OrderEventLogEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(OrderEventLogService.class);

	@Autowired
	private OrderEventLogRepository orderEventLogRepository;

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEventLogEntity save(OrderEventLogEntity entity) {
		OrderEventLogEntity result = orderEventLogRepository.save(entity);
		return result;
	}

	@Override
	public Class<OrderEventLogEntity> getEntity() {
		return OrderEventLogEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return orderEventLogRepository;
	}

}