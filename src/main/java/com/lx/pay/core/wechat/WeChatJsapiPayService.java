package com.lx.pay.core.wechat;

import cn.hutool.core.date.DatePattern;
import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.core.TradeResult;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.exception.CustomizeException;
import com.wechat.pay.java.core.util.GsonUtil;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 微信JSAPI支付实现类
 *
 * @author chenhaizhuang
 */
@Service("wechatJSAPI")
public class WeChatJsapiPayService extends WeChatPayService {

    @Autowired
    public void setWeChatPayProperties(WeChatPayProperties weChatPayProperties) {
        this.weChatPayProperties = weChatPayProperties;
    }

    @Autowired
    public void setNotifyUrlProperties(NotifyUrlProperties notifyUrlProperties) {
        this.notifyUrlProperties = notifyUrlProperties;
    }

    @Override
    @SuppressWarnings("all")
    public String prepay(PayTrade payTrade) {
        String in = payTrade.getIn();

        Amount amount = new Amount();
        amount.setTotal(payTrade.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        Payer payer = new Payer();
        payer.setOpenid(getOpenId(payTrade));

        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(weChatPayProperties.getIns().get(in));
        prepayRequest.setMchid(WeChatPayFactory.getMerchant(weChatPayProperties, in).getMerchantId());
        prepayRequest.setDescription(payTrade.getDescription());
        prepayRequest.setOutTradeNo(payTrade.getTradeNo());
        prepayRequest.setTimeExpire(payTrade.getExpireTime().atOffset(ZoneOffset.of("+08:00"))
                .format(DateTimeFormatter.ofPattern(DatePattern.UTC_WITH_XXX_OFFSET_PATTERN)));
        prepayRequest.setNotifyUrl(notifyUrlProperties.getPayNotifyUrl(in));
        prepayRequest.setAmount(amount);
        prepayRequest.setPayer(payer);

        try {
            PrepayWithRequestPaymentResponse prepayResponse = WeChatPayFactory
                    .getJsapiServiceExtension(weChatPayProperties, in)
                    .prepayWithRequestPayment(prepayRequest);
            return GsonUtil.toJson(prepayResponse);
        } catch (Exception e) {
            throw new CustomizeException("微信JSAPI支付 预支付失败", e);
        }
    }

    private String getOpenId(PayTrade payTrade) {
        // 用户经过小程序登录后，在用户服务上保存了用户的openId
        // 在此可通过入口、用户ID等信息获取到对于的openId
        return "";
    }

    @Override
    public void closePay(PayTrade payTrade) {
        String in = payTrade.getIn();

        CloseOrderRequest closeOrderRequest = new CloseOrderRequest();
        closeOrderRequest.setMchid(WeChatPayFactory.getMerchant(weChatPayProperties, in).getMerchantId());
        closeOrderRequest.setOutTradeNo(payTrade.getTradeNo());

        try {
            WeChatPayFactory.getJsapiServiceExtension(weChatPayProperties, in).closeOrder(closeOrderRequest);
        } catch (Exception e) {
            throw new CustomizeException("微信JSAPI支付 关闭支付失败", e);
        }
    }

    @Override
    public TradeResult queryPay(PayTrade payTrade) {
        String in = payTrade.getIn();

        QueryOrderByOutTradeNoRequest queryOrderByOutTradeNoRequest = new QueryOrderByOutTradeNoRequest();
        queryOrderByOutTradeNoRequest.setMchid(WeChatPayFactory.getMerchant(weChatPayProperties, in).getMerchantId());
        queryOrderByOutTradeNoRequest.setOutTradeNo(payTrade.getTradeNo());

        try {
            Transaction transaction = WeChatPayFactory.getJsapiServiceExtension(weChatPayProperties, in)
                    .queryOrderByOutTradeNo(queryOrderByOutTradeNoRequest);

            return buildTradeResult(transaction);
        } catch (Exception e) {
            throw new CustomizeException("微信JSAPI支付 查询支付失败", e);
        }
    }
}
