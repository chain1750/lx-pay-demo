package com.lx.pay.core;

import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.dao.entity.RefundTrade;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付核心接口<br>
 * 所有的支付方式都会实现这个接口，支付服务会根据入口来自动获取实现类
 *
 * @author chenhaizhuang
 */
public interface IPayService {

    /**
     * 预支付
     *
     * @param payTrade 支付交易
     * @return String
     */
    String prepay(PayTrade payTrade);

    /**
     * 关闭支付
     *
     * @param payTrade 支付交易
     */
    void closePay(PayTrade payTrade);

    /**
     * 查询支付
     *
     * @param payTrade 支付交易
     * @return TradeResult
     */
    TradeResult queryPay(PayTrade payTrade);

    /**
     * 解析支付通知
     *
     * @param request 通知请求
     * @param in      入口
     * @return TradeResult
     */
    TradeResult parsePayNotify(HttpServletRequest request, String in);

    /**
     * 退款
     *
     * @param refundTrade 退款交易
     */
    void refund(RefundTrade refundTrade);

    /**
     * 查询退款
     *
     * @param refundTrade 退款交易
     * @return TradeResult
     */
    TradeResult queryRefund(RefundTrade refundTrade);

    /**
     * 解析退款通知
     *
     * @param request 通知请求
     * @param in      入口
     * @return TradeResult
     */
    TradeResult parseRefundNotify(HttpServletRequest request, String in);
}
