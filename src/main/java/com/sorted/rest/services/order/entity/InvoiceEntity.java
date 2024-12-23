package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.ConsumerInvoiceDataBean;
import com.sorted.rest.services.order.beans.InvoiceDataBean;
import com.sorted.rest.services.order.beans.PaymentNoteDetails;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = OrderConstants.INVOICES_TABLE_NAME)
@DynamicUpdate
@Data
@Where(clause = "active = 1")
public class InvoiceEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column
	private String entityId;

	@Column(name = "display_order_id")
	private String displayOrderId;

	@Column
	private String invoiceName;

	@Column
	private String invoiceUrl;

	@Column
	private String invoiceType;

	@Column
	private Date deliveryDate;

	@Column
	private String dateString;

	@Column
	private Long invoiceId;

	@Column
	private Double amount;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private PaymentNoteDetails paymentNotes = new PaymentNoteDetails();

	public static InvoiceEntity newInstance() {
		return new InvoiceEntity();
	}

	public static InvoiceEntity createInvoiceEntity(FranchiseOrderEntity franchiseOrder, InvoiceDataBean invoiceData, String dateString, Long invoiceId) {
		InvoiceEntity invoice = newInstance();
		invoice.setDisplayOrderId(franchiseOrder.getDisplayOrderId());
		invoice.setEntityId(franchiseOrder.getStoreId());
		invoice.setInvoiceType("FRANCHISE");
		invoice.setDeliveryDate(franchiseOrder.getDeliveryDate());
		invoice.setDateString(dateString);
		invoice.setInvoiceId(invoiceId);
		return invoice;
	}

	public static InvoiceEntity createConsumerInvoiceEntity(OrderEntity order, String dateString, Long invoiceId, String invoiceName) {
		InvoiceEntity invoice = newInstance();
		invoice.setDisplayOrderId(order.getDisplayOrderId());
		invoice.setEntityId(order.getCustomerId().toString());
		invoice.setInvoiceType("CONSUMER");
		invoice.setDeliveryDate(order.getDeliveryDate());
		invoice.setDateString(dateString);
		invoice.setInvoiceId(invoiceId);
		invoice.setInvoiceName(invoiceName);
		return invoice;
	}
}
