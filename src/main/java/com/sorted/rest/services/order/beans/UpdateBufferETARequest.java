package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "Update Additional Buffer time")
public class UpdateBufferETARequest {

    private static final long serialVersionUID = -8927594493563844997L;

    @NotNull (message= "Additional Buffer time can not be empty")
    @Range(min = 0)
    private Integer  additionalBufferTime;
}