package com.lx.pay.controller;

import com.lx.pay.model.IResult;
import com.lx.pay.model.req.ClosePayReq;
import com.lx.pay.model.req.PrepayReq;
import com.lx.pay.model.req.QueryPayReq;
import com.lx.pay.model.req.RefundReq;
import com.lx.pay.model.resp.PrepayResp;
import com.lx.pay.model.resp.QueryPayResp;
import com.lx.pay.model.resp.RefundResp;
import com.lx.pay.service.BizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 业务接口<br>
 * 业务方主要对接该接口。
 *
 * @author chenhaizhuang
 */
@RestController
public class BizController {

    @Autowired
    private BizService bizService;

    /**
     * 预支付
     *
     * @param req 请求
     * @return IResult
     */
    @PostMapping("/prepay")
    public IResult<PrepayResp> prepay(@Valid @RequestBody PrepayReq req) {
        return IResult.success(bizService.prepay(req));
    }

    /**
     * 关闭支付
     *
     * @param req 请求
     * @return IResult
     */
    @PostMapping("/closePay")
    public IResult<Void> closePay(@Valid @RequestBody ClosePayReq req) {
        bizService.closePay(req);
        return IResult.success();
    }

    /**
     * 查询支付
     *
     * @param req 请求
     * @return IResult
     */
    @PostMapping("/queryPay")
    public IResult<QueryPayResp> queryPay(@Valid @RequestBody QueryPayReq req) {
        return IResult.success(bizService.queryPay(req));
    }

    /**
     * 退款
     *
     * @param req 请求
     * @return IResult
     */
    @PostMapping("/refund")
    public IResult<RefundResp> refund(@Valid @RequestBody RefundReq req) {
        return IResult.success(bizService.refund(req));
    }
}
