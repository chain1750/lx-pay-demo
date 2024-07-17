package com.lx.pay.model.resp;

import lombok.Data;

/**
 * 预支付结果
 *
 * @author chenhaizhuang
 */
@Data
public class PrepayResp {

    /**
     * 交易编号，交易的唯一标识
     */
    private String tradeNo;

    /**
     * 预支付信息，各种支付方式会有不同的实现，都转为字符串返回
     */
    private String prepay;
}
