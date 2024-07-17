package com.lx.pay.service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author chenhaizhuang
 */
public interface NotifyService {

    /**
     * 支付通知
     *
     * @param request 通知请求
     * @param in      入口
     * @return String
     */
    String payNotify(HttpServletRequest request, String in);

    /**
     * 退款通知
     *
     * @param request 通知请求
     * @param in      入口
     * @return String
     */
    String refundNotify(HttpServletRequest request, String in);
}
