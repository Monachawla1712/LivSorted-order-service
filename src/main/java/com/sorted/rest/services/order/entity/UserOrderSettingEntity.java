package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = OrderConstants.USER_ORDER_SETTING_TABLE_NAME)
@Data
public class UserOrderSettingEntity extends BaseEntity {

	private static final long serialVersionUID = -4989971602403123933L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column
	UUID customerId;

	@Column
	String key;

	@Column
	String value;

	public static UserOrderSettingEntity createNewEntity(UUID customerId, String key, String value, UUID createdBy, UUID modifiedBy) {
		UserOrderSettingEntity userOrderSettingEntity = new UserOrderSettingEntity();
		userOrderSettingEntity.setCustomerId(customerId);
		userOrderSettingEntity.setKey(key);
		userOrderSettingEntity.setValue(value);
		userOrderSettingEntity.setCreatedBy(createdBy);
		userOrderSettingEntity.setModifiedBy(modifiedBy);
		return userOrderSettingEntity;
	}

	public static class Keys {

		public static final String isAutoCheckoutEnabled = "isAutoCheckoutEnabled";
	}

}
