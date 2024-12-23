package com.sorted.rest.services.order.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.PreBookOrderEntity;

@Repository
public interface PreBookOrderRepository extends BaseCrudRepository<PreBookOrderEntity, UUID> {

}
