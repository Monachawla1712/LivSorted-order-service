package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.RejectionOrderEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RejectionOrderRepository extends BaseCrudRepository<RejectionOrderEntity, UUID> {

}
