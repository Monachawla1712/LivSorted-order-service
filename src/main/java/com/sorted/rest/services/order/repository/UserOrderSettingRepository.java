package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.UserOrderSettingEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOrderSettingRepository extends BaseCrudRepository<UserOrderSettingEntity, Long> {

	Optional<UserOrderSettingEntity> findByCustomerIdAndKey(UUID customerId, String key);
}
