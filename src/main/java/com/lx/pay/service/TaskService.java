package com.lx.pay.service;

/**
 * @author chenhaizhuang
 */
public interface TaskService {

    /**
     * 处理未支付
     */
    void handleNotPay();

    /**
     * 处理退款中
     */
    void handleInRefund();
}
