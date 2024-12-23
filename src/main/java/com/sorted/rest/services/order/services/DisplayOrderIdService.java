package com.sorted.rest.services.order.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.ContextUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.entity.DisplayOrderIdEntity;
import com.sorted.rest.services.order.repository.DisplayOrderIdRepository;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Abhishek on 29.9.22.
 */
@Service
public class DisplayOrderIdService implements BaseService<DisplayOrderIdEntity> {

	private final AppLogger _LOGGER = LoggingManager.getLogger(DisplayOrderIdService.class);

	@Autowired
	private DisplayOrderIdRepository displayOrderIdRepository;

	@Override
	public Class<DisplayOrderIdEntity> getEntity() {
		return DisplayOrderIdEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return displayOrderIdRepository;
	}

	private BlockingQueue<String> displayOrderIdQueue = new LinkedBlockingQueue<>();

	public DisplayOrderIdEntity fetchFirst() {
		Iterable<DisplayOrderIdEntity> resultOpt = displayOrderIdRepository.findAll(null, null, 1, 1);
		return resultOpt.iterator().next();
	}

	public synchronized BlockingQueue<String> fetchBulkAndInactive() {
		if (displayOrderIdQueue.isEmpty()) {
			Integer limit = 10;
			if (ContextUtils.isProfileProd()) {
				limit = 1000;
			}
			List<String> ids = displayOrderIdRepository.getBulkDisplayOrderIds(limit);
			return getResultQueue(ids);
		} else {
			return displayOrderIdQueue;
		}
	}

	private BlockingQueue<String> getResultQueue(List<String> list) {
		list.forEach(displayOrderIdQueue::offer);
		return displayOrderIdQueue;
	}

	private String fetchOneAndInactivate() {
		DisplayOrderIdEntity entity = null;
		Integer rowsUpdated = 0;

		while (rowsUpdated == 0) {
			entity = fetchFirst();
			rowsUpdated = displayOrderIdRepository.inactivateDisplayOrderId(entity.getDisplayOrderId());
		}
		return entity.getDisplayOrderId();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String getNewDisplayOrderId() {
		try {
			String displayOrderId = fetchFromQueue();
			if (displayOrderId != null) {
				return displayOrderId;
			}
		} catch (Exception e) {
			_LOGGER.error("Error while fetching display order id from queue", e);
		}
		_LOGGER.info("getNewDisplayOrderId :: fetchFromQueue returned null");
		return fetchOneAndInactivate();
	}

	private String fetchFromQueue() throws InterruptedException {
		if (displayOrderIdQueue.isEmpty()) {
			_LOGGER.info("fetchFromQueue :: displayOrderIdQueue is empty");
			displayOrderIdQueue = fetchBulkAndInactive();
		}
		return displayOrderIdQueue.take();
	}
}