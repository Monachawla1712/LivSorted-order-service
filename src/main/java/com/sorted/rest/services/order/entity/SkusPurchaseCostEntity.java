package com.sorted.rest.services.order.entity;


import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = OrderConstants.SKUS_PURCHASE_COST_TABLE_NAME)
@DynamicUpdate
@Data
public class SkusPurchaseCostEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column
	private Date deliveryDate;

	@Column
	private String skuCode;

	@Column
	private Double avgCostPrice;

	@Column
	private Double purchaseQuantity;

	@Column
	private Double mrp;

	public static SkusPurchaseCostEntity newInstance() {
		SkusPurchaseCostEntity entity = new SkusPurchaseCostEntity();
		return entity;
	}
}
