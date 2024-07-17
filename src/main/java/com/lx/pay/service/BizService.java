package com.lx.pay.service;

import com.lx.pay.model.req.ClosePayReq;
import com.lx.pay.model.req.PrepayReq;
import com.lx.pay.model.req.QueryPayReq;
import com.lx.pay.model.req.RefundReq;
import com.lx.pay.model.resp.PrepayResp;
import com.lx.pay.model.resp.QueryPayResp;
import com.lx.pay.model.resp.RefundResp;

/**
 * @author chenhaizhuang
 */
public interface BizService {

    /**
     * 预支付
     *
     * @param req 请求
     * @return PrepayResp
     */
    PrepayResp prepay(PrepayReq req);

    /**
     * 关闭支付
     *
     * @param req 请求
     */
    void closePay(ClosePayReq req);

    /**
     * 查询支付
     *
     * @param req 请求
     * @return QueryPayResp
     */
    QueryPayResp queryPay(QueryPayReq req);

    /**
     * 退款
     *
     * @param req 请求
     * @return RefundResp
     */
    RefundResp refund(RefundReq req);
}
