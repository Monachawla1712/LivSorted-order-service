package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderItemLogType;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = OrderConstants.FRANCHISE_ORDER_ITEM_LOGS_TABLE_NAME)
@DynamicUpdate
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FranchiseOrderItemLogEntity extends BaseEntity {

	private static final long serialVersionUID = 8485511358572467824L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column(nullable = false)
	private String skuCode;

	@Column(nullable = false)
	private String storeId;

	@Column(nullable = false)
	private Integer fromQty;

	@Column(nullable = false)
	private Integer toQty;

	@Column()
	private UUID orderId;

	@Column(nullable = false)
	private FranchiseOrderItemLogType type = FranchiseOrderItemLogType.USER_CHANGE;

	public static FranchiseOrderItemLogEntity newInstance() {
		return new FranchiseOrderItemLogEntity();
	}

}
