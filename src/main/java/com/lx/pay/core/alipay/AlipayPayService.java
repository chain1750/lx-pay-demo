package com.lx.pay.core.alipay;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.extra.servlet.ServletUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.lx.pay.core.IPayService;
import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.core.TradeResult;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.dao.entity.RefundTrade;
import com.lx.pay.enums.PayStatusEnum;
import com.lx.pay.enums.RefundStatusEnum;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付宝支付实现类<br>
 * 支付宝支付有多个实现方法是一致的，采用基类实现。
 *
 * @author chenhaizhuang
 */
@Slf4j
public abstract class AlipayPayService implements IPayService {

    protected AlipayPayProperties alipayPayProperties;

    protected NotifyUrlProperties notifyUrlProperties;

    @Override
    public void closePay(PayTrade payTrade) {
        AlipayClient alipayClient = AlipayPayFactory.getAlipayClient(alipayPayProperties, payTrade.getIn());

        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizModel(model);

        AlipayTradeCloseResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (Exception e) {
            throw new RuntimeException("支付宝支付 关闭支付失败", e);
        }
        Assert.isTrue(response.isSuccess() || "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode()),
                "支付宝支付 关闭支付失败：" + response.getSubMsg());
    }

    @Override
    public TradeResult queryPay(PayTrade payTrade) {
        AlipayClient alipayClient = AlipayPayFactory.getAlipayClient(alipayPayProperties, payTrade.getIn());

        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);

        AlipayTradeQueryResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (Exception e) {
            throw new RuntimeException("支付宝支付 查询支付失败", e);
        }
        Assert.isTrue(response.isSuccess() || "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode()),
                "支付宝支付 查询支付失败：" + response.getSubMsg());
        if (!response.isSuccess() && "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
            response.setOutTradeNo(payTrade.getTradeNo());
            response.setTradeStatus("WAIT_BUYER_PAY");
        }

        return buildTradeResult(response);
    }

    @Override
    @SuppressWarnings("all")
    public TradeResult parsePayNotify(HttpServletRequest request, String in) {
        Map<String, String> requestParam = new HashMap<>();
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String name = entry.getKey();
            if ("sign_type".equals(name)) {
                continue;
            }
            requestParam.put(name, URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8));
        }
        AlipayPayProperties.Account account = AlipayPayFactory.getAccount(alipayPayProperties, in);
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV2(requestParam, account.getPublicKey(), "utf-8", "RSA2");
        } catch (Exception e) {
            throw new RuntimeException("支付宝支付 解析支付通知失败", e);
        }
        String sellerId = requestParam.get("seller_id");
        Assert.isTrue(signVerified && account.getSellerId().equals(sellerId), "支付宝支付 解析支付通知失败");

        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setOutTradeNo(requestParam.get("out_trade_no"));
        response.setTradeNo(requestParam.get("trade_no"));
        response.setTradeStatus(requestParam.get("trade_status"));
        response.setSendPayDate(DateUtil.parse(requestParam.get("gmt_payment"), DatePattern.NORM_DATETIME_PATTERN));
        log.info("支付宝支付 支付通知结果：{}", response);
        return buildTradeResult(response);
    }

    @Override
    public void refund(RefundTrade refundTrade) {
        PayTrade payTrade = refundTrade.getPayTrade();

        AlipayClient alipayClient = AlipayPayFactory.getAlipayClient(alipayPayProperties, payTrade.getIn());
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        model.setRefundAmount(refundTrade.getAmount().toString());
        model.setRefundReason(refundTrade.getDescription());
        model.setOutRequestNo(refundTrade.getTradeNo());
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);
        request.setNotifyUrl(notifyUrlProperties.getRefundNotifyUrl(payTrade.getIn()));

        AlipayTradeRefundResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (Exception e) {
            throw new RuntimeException("支付宝支付 退款失败", e);
        }
        Assert.isTrue(response.isSuccess(), "支付宝支付 退款失败：" + response.getSubMsg());
    }

    @Override
    public TradeResult queryRefund(RefundTrade refundTrade) {
        PayTrade payTrade = refundTrade.getPayTrade();

        AlipayClient alipayClient = AlipayPayFactory.getAlipayClient(alipayPayProperties, payTrade.getIn());
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        model.setOutRequestNo(refundTrade.getTradeNo());
        model.setQueryOptions(List.of("gmt_refund_pay"));
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        request.setBizModel(model);

        AlipayTradeFastpayRefundQueryResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (Exception e) {
            throw new RuntimeException("支付宝支付 查询退款失败", e);
        }
        Assert.isTrue(response.isSuccess(), "支付宝支付 查询退款失败：" + response.getSubMsg());
        return buildTradeResult(response);
    }

    @Override
    @SuppressWarnings("all")
    public TradeResult parseRefundNotify(HttpServletRequest request, String in) {
        Map<String, String> requestParam = new HashMap<>();
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String name = entry.getKey();
            if ("sign_type".equals(name)) {
                continue;
            }
            requestParam.put(name, URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8));
        }
        AlipayPayProperties.Account account = AlipayPayFactory.getAccount(alipayPayProperties, in);
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV2(requestParam, account.getPublicKey(), "utf-8", "RSA2");
        } catch (Exception e) {
            throw new RuntimeException("支付宝支付 解析退款通知失败", e);
        }

        String sellerId = requestParam.get("seller_id");
        Assert.isTrue(signVerified && account.getSellerId().equals(sellerId), "支付宝支付 解析退款通知失败");

        String tradeStatus = requestParam.get("trade_status");
        String refundStatus = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_CLOSED".equals(tradeStatus)
                ? "REFUND_SUCCESS" : null;
        AlipayTradeFastpayRefundQueryResponse response = new AlipayTradeFastpayRefundQueryResponse();
        response.setOutRequestNo(requestParam.get("out_biz_no"));
        response.setRefundStatus(refundStatus);
        response.setGmtRefundPay(DateUtil.parse(requestParam.get("gmt_payment"), DatePattern.NORM_DATETIME_MS_FORMAT));
        log.info("支付宝支付 退款通知结果：{}", response);
        return buildTradeResult(response);
    }

    /**
     * 构建交易结果
     *
     * @param response 查询支付结果
     * @return TradeResult
     */
    private TradeResult buildTradeResult(AlipayTradeQueryResponse response) {
        TradeResult tradeResult = new TradeResult();
        tradeResult.setTradeNo(response.getOutTradeNo());
        tradeResult.setOutTradeNo(response.getTradeNo());
        tradeResult.setDefaultNotifyResult("success");

        String tradeStatus = response.getTradeStatus();
        if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
            tradeResult.setStatus(PayStatusEnum.NOT_PAY.getValue());
        } else if ("TRADE_SUCCESS".equals(tradeStatus)) {
            tradeResult.setStatus(PayStatusEnum.PAY_SUCCESS.getValue());
            tradeResult.setFinishTime(LocalDateTimeUtil.of(response.getSendPayDate()));
        } else {
            tradeResult.setStatus(PayStatusEnum.PAY_CLOSED.getValue());
        }

        return tradeResult;
    }

    /**
     * 构建交易结果
     *
     * @param response 查询退款结果
     * @return TradeResult
     */
    private TradeResult buildTradeResult(AlipayTradeFastpayRefundQueryResponse response) {
        TradeResult tradeResult = new TradeResult();
        tradeResult.setTradeNo(response.getOutRequestNo());
        tradeResult.setOutTradeNo(response.getTradeNo());
        tradeResult.setDefaultNotifyResult("success");

        String refundStatus = response.getRefundStatus();
        if ("REFUND_SUCCESS".equals(refundStatus)) {
            tradeResult.setStatus(RefundStatusEnum.REFUND_SUCCESS.getValue());
            tradeResult.setFinishTime(LocalDateTimeUtil.of(response.getGmtRefundPay()));
        } else {
            tradeResult.setStatus(RefundStatusEnum.IN_REFUND.getValue());
        }

        return tradeResult;
    }
}
