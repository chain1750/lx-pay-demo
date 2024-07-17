package com.lx.pay.controller;

import com.lx.pay.model.IResult;
import com.lx.pay.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 通知接口<br>
 * 该接口将作为接收微信、支付宝通知的回调接口，配合发起预支付时传的通知地址使用。<br>
 * 支付服务不应该对外开放，所以返回参数采用统一接口结果，应在开放层把接口结果的data字段取出返回给微信、支付宝。
 *
 * @author chenhaizhuang
 */
@RestController
@RequestMapping("/notify")
public class NotifyController {

    @Autowired
    private NotifyService notifyService;

    /**
     * 支付通知
     *
     * @param request 通知请求
     * @param in      入口
     * @return IResult
     */
    @PostMapping("/pay/{in}")
    public IResult<String> payNotify(HttpServletRequest request, @PathVariable String in) {
        return IResult.success(notifyService.payNotify(request, in));
    }

    /**
     * 退款通知
     *
     * @param request 通知请求
     * @param in      入口
     * @return IResult
     */
    @PostMapping("/refund/{in}")
    public IResult<String> refundNotify(HttpServletRequest request, @PathVariable String in) {
        return IResult.success(notifyService.refundNotify(request, in));
    }
}
