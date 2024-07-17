package com.lx.pay.model.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款结果
 *
 * @author chenhaizhuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResp {

    /**
     * 交易编号，交易的唯一标识
     */
    private String tradeNo;
}
