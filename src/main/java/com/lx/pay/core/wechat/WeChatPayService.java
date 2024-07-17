package com.lx.pay.core.wechat;

import cn.hutool.core.date.DatePattern;
import cn.hutool.extra.servlet.ServletUtil;
import com.lx.pay.core.IPayService;
import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.core.TradeResult;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.dao.entity.RefundTrade;
import com.lx.pay.enums.PayStatusEnum;
import com.lx.pay.enums.RefundStatusEnum;
import com.lx.pay.exception.CustomizeException;
import com.wechat.pay.java.core.http.Constant;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 微信支付实现类<br>
 * 微信支付有多个实现方法是一致的，采用基类实现。
 *
 * @author chenhaizhuang
 */
@Slf4j
public abstract class WeChatPayService implements IPayService {

    protected static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(DatePattern.UTC_WITH_XXX_OFFSET_PATTERN);

    protected WeChatPayProperties weChatPayProperties;

    protected NotifyUrlProperties notifyUrlProperties;

    @Override
    public TradeResult parsePayNotify(HttpServletRequest request, String in) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(request.getHeader(Constant.WECHAT_PAY_SERIAL))
                    .nonce(request.getHeader(Constant.WECHAT_PAY_NONCE))
                    .signature(request.getHeader(Constant.WECHAT_PAY_SIGNATURE))
                    .timestamp(request.getHeader(Constant.WECHAT_PAY_TIMESTAMP))
                    .body(ServletUtil.getBody(request))
                    .build();
            log.info("微信支付 支付通知参数：{}", requestParam);

            Transaction transaction = WeChatPayFactory.getNotificationParser(weChatPayProperties, in)
                    .parse(requestParam, Transaction.class);
            log.info("微信支付 支付通知结果：{}", transaction);

            return buildTradeResult(transaction);
        } catch (Exception e) {
            throw new CustomizeException("微信支付 解析支付通知失败", e);
        }
    }

    @Override
    public void refund(RefundTrade refundTrade) {
        PayTrade payTrade = refundTrade.getPayTrade();
        String in = payTrade.getIn();

        AmountReq amountReq = new AmountReq();
        amountReq.setRefund(refundTrade.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        amountReq.setTotal(payTrade.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        amountReq.setCurrency("CNY");

        CreateRequest createRequest = new CreateRequest();
        createRequest.setOutTradeNo(payTrade.getTradeNo());
        createRequest.setOutRefundNo(refundTrade.getTradeNo());
        createRequest.setReason(refundTrade.getDescription());
        createRequest.setAmount(amountReq);
        createRequest.setNotifyUrl(notifyUrlProperties.getRefundNotifyUrl(in));

        try {
            WeChatPayFactory.getRefundService(weChatPayProperties, in).create(createRequest);
        } catch (Exception e) {
            throw new CustomizeException("微信支付 退款失败", e);
        }
    }

    @Override
    public TradeResult queryRefund(RefundTrade refundTrade) {
        PayTrade payTrade = refundTrade.getPayTrade();
        QueryByOutRefundNoRequest queryByOutRefundNoRequest = new QueryByOutRefundNoRequest();
        queryByOutRefundNoRequest.setOutRefundNo(refundTrade.getTradeNo());

        try {
            Refund refund = WeChatPayFactory.getRefundService(weChatPayProperties, payTrade.getIn())
                    .queryByOutRefundNo(queryByOutRefundNoRequest);

            return buildTradeResult(refund);
        } catch (Exception e) {
            throw new CustomizeException("微信支付 查询退款失败", e);
        }
    }

    @Override
    public TradeResult parseRefundNotify(HttpServletRequest request, String in) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(request.getHeader(Constant.WECHAT_PAY_SERIAL))
                    .nonce(request.getHeader(Constant.WECHAT_PAY_NONCE))
                    .signature(request.getHeader(Constant.WECHAT_PAY_SIGNATURE))
                    .timestamp(request.getHeader(Constant.WECHAT_PAY_TIMESTAMP))
                    .body(ServletUtil.getBody(request))
                    .build();
            log.info("微信支付 退款通知参数：{}", requestParam);

            RefundNotification refundNotification = WeChatPayFactory
                    .getNotificationParser(weChatPayProperties, in)
                    .parse(requestParam, RefundNotification.class);
            log.info("微信支付 退款通知结果：{}", refundNotification);

            Refund refund = new Refund();
            refund.setOutRefundNo(refundNotification.getOutRefundNo());
            refund.setRefundId(refundNotification.getRefundId());
            refund.setStatus(refundNotification.getRefundStatus());
            refund.setSuccessTime(refundNotification.getSuccessTime());
            return buildTradeResult(refund);
        } catch (Exception e) {
            throw new CustomizeException("微信支付 解析退款通知失败", e);
        }
    }

    /**
     * 构建交易结果
     *
     * @param transaction 微信支付交易
     * @return TradeResult
     */
    protected TradeResult buildTradeResult(Transaction transaction) {
        TradeResult tradeResult = new TradeResult();
        tradeResult.setTradeNo(transaction.getOutTradeNo());
        tradeResult.setOutTradeNo(transaction.getTransactionId());
        tradeResult.setDefaultNotifyResult("");

        Transaction.TradeStateEnum tradeState = transaction.getTradeState();
        if (Transaction.TradeStateEnum.NOTPAY == tradeState) {
            tradeResult.setStatus(PayStatusEnum.NOT_PAY.getValue());
        } else if (Transaction.TradeStateEnum.SUCCESS == tradeState) {
            tradeResult.setStatus(PayStatusEnum.PAY_SUCCESS.getValue());
            tradeResult.setFinishTime(OffsetDateTime
                    .parse(transaction.getSuccessTime(), FORMATTER)
                    .toLocalDateTime());
        } else {
            tradeResult.setStatus(PayStatusEnum.PAY_CLOSED.getValue());
        }

        return tradeResult;
    }

    /**
     * 构建交易结果
     *
     * @param refund 微信退款交易
     * @return TradeResult
     */
    private TradeResult buildTradeResult(Refund refund) {
        TradeResult tradeResult = new TradeResult();
        tradeResult.setTradeNo(refund.getOutRefundNo());
        tradeResult.setOutTradeNo(refund.getRefundId());
        tradeResult.setDefaultNotifyResult("");

        Status status = refund.getStatus();
        if (Status.PROCESSING == status) {
            tradeResult.setStatus(RefundStatusEnum.IN_REFUND.getValue());
        } else if (Status.SUCCESS == status) {
            tradeResult.setStatus(RefundStatusEnum.REFUND_SUCCESS.getValue());
            tradeResult.setFinishTime(OffsetDateTime
                    .parse(refund.getSuccessTime(), FORMATTER)
                    .toLocalDateTime());
        } else {
            tradeResult.setStatus(RefundStatusEnum.REFUND_FAIL.getValue());
        }

        return tradeResult;
    }
}
