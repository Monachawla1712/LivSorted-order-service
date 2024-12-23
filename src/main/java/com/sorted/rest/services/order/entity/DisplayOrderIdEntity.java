package com.sorted.rest.services.order.entity;

import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Abhishek
 */
@Entity
@Table(name = OrderConstants.DISPLAY_ORDER_ID)
@DynamicUpdate
@Data
@Where(clause = "active = 1")
public class DisplayOrderIdEntity {

    private static final long serialVersionUID = -7538803140039235801L;

    @Id
    @Column(nullable = false)
    private String displayOrderId;

    @Column(nullable = false)
    private int active;

}