package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sorted.rest.common.beans.ErrorBean;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
@ApiModel(description = "Franchise Store Cart Request Bean")
@Data
public class FranchiseCartResponse implements Serializable {
    private static final long serialVersionUID = 6369074118582415131L;

    private FranchiseOrderResponseBean data;

    /**
     * true implies successful execution
     *
     */
    private boolean status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ErrorBean error;

    public FranchiseCartResponse error(String error, String errorMessage) {
        this.error = new ErrorBean(error, errorMessage);
        this.message = errorMessage;
        this.status = false;
        return this;
    }

    public void setData(FranchiseOrderResponseBean data) {
        this.data = data;
        this.status = true;
    }
}
