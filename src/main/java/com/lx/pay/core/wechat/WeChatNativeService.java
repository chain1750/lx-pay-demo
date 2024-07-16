package com.lx.pay.core.wechat;

import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.core.TradeResult;
import com.lx.pay.dao.entity.PayTrade;
import com.wechat.pay.java.core.util.GsonUtil;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneOffset;

/**
 * 微信Native支付实现类
 *
 * @author chenhaizhuang
 */
@Service("wechatNative")
public class WeChatNativeService extends WeChatPayService {

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

        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(weChatPayProperties.getIns().get(in));
        prepayRequest.setMchid(WeChatPayFactory.getMerchant(weChatPayProperties, in).getMerchantId());
        prepayRequest.setDescription(payTrade.getDescription());
        prepayRequest.setOutTradeNo(payTrade.getTradeNo());
        prepayRequest.setTimeExpire(payTrade.getExpireTime().atOffset(ZoneOffset.of("+08:00")).format(FORMATTER));
        prepayRequest.setNotifyUrl(notifyUrlProperties.getPayNotifyUrl(in));
        prepayRequest.setAmount(amount);

        try {
            PrepayResponse prepayResponse = WeChatPayFactory.getNativePayService(weChatPayProperties, in)
                    .prepay(prepayRequest);
            return GsonUtil.toJson(prepayResponse);
        } catch (Exception e) {
            throw new RuntimeException("微信Native支付 预支付失败", e);
        }
    }

    @Override
    public void closePay(PayTrade payTrade) {
        String in = payTrade.getIn();

        CloseOrderRequest closeOrderRequest = new CloseOrderRequest();
        closeOrderRequest.setMchid(WeChatPayFactory.getMerchant(weChatPayProperties, in).getMerchantId());
        closeOrderRequest.setOutTradeNo(payTrade.getTradeNo());

        try {
            WeChatPayFactory.getNativePayService(weChatPayProperties, in).closeOrder(closeOrderRequest);
        } catch (Exception e) {
            throw new RuntimeException("微信Native支付 关闭支付失败", e);
        }
    }

    @Override
    public TradeResult queryPay(PayTrade payTrade) {
        String in = payTrade.getIn();

        QueryOrderByOutTradeNoRequest queryOrderByOutTradeNoRequest = new QueryOrderByOutTradeNoRequest();
        queryOrderByOutTradeNoRequest.setMchid(WeChatPayFactory.getMerchant(weChatPayProperties, in).getMerchantId());
        queryOrderByOutTradeNoRequest.setOutTradeNo(payTrade.getTradeNo());

        try {
            Transaction transaction = WeChatPayFactory.getNativePayService(weChatPayProperties, in)
                    .queryOrderByOutTradeNo(queryOrderByOutTradeNoRequest);

            return buildTradeResult(transaction);
        } catch (Exception e) {
            throw new RuntimeException("微信Native支付 查询支付失败", e);
        }
    }
}
