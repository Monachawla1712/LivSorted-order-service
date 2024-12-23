package com.sorted.rest.services.order.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.entity.PreBookOrderEntity;
import com.sorted.rest.services.order.repository.PreBookOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class PreBookOrderService implements BaseService<PreBookOrderEntity> {

	@Autowired
	private PreBookOrderRepository preBookOrderRepository;

	public void saveAllPrebookOrders(List<PreBookOrderEntity> preBookOrders) {
		preBookOrderRepository.saveAll(preBookOrders);
	}

	public List<PreBookOrderEntity> findPrebookedOrdersBySku(String customerId, String skuCode, Date prebookDeliveryDate) {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("customerId", customerId);
		filters.put("skuCode", skuCode);
		filters.put("prebookDeliveryDate", prebookDeliveryDate);
		return findAllRecords(filters);
	}

	public List<PreBookOrderEntity> findPrebookedOrdersByDeliveryDate(Date prebookDeliveryDate) {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("prebookDeliveryDate", prebookDeliveryDate);
		return findAllRecords(filters);
	}

	@Override
	public Class<PreBookOrderEntity> getEntity() {
		return PreBookOrderEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return preBookOrderRepository;
	}
}
