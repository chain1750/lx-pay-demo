package com.lx.pay.core;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易结果
 *
 * @author chenhaizhuang
 */
@Data
public class TradeResult {

    /**
     * 交易编号
     */
    private String tradeNo;

    /**
     * 外部交易编号
     */
    private String outTradeNo;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 默认回调通知结果<br>
     * 回调通知到支付服务上时，支付服务采用异步执行的方式，故仅需返回成功的响应数据即可
     */
    @JSONField(serialize = false)
    private String defaultNotifyResult;
}
