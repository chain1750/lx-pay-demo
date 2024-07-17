package com.lx.pay.model.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 查询支付请求
 *
 * @author chenhaizhuang
 */
@Data
public class QueryPayReq {

    /**
     * 交易编号，交易的唯一标识
     */
    @NotBlank(message = "交易编号不能为空")
    private String tradeNo;
}
