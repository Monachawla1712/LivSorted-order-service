package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * The Interface OrderItemRepository.
 */
@Repository
public interface InvoiceRepository extends BaseCrudRepository<InvoiceEntity, Integer> {

	InvoiceEntity findByDisplayOrderIdAndActive(String displayOrderId, Integer active);

	@Procedure(procedureName = "generate_invoice_id")
	Long getGeneratedInvoiceId(@Param("date_string_param") String dateString, @Param("invoice_type_param") String invoiceType);
}
