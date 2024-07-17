package com.lx.pay.model.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 关闭支付请求
 *
 * @author chenhaizhuang
 */
@Data
public class ClosePayReq {

    /**
     * 交易编号，交易的唯一标识
     */
    @NotBlank(message = "交易编号不能为空")
    private String tradeNo;
}
