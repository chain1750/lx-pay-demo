package com.lx.pay.core.alipay;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Assert;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.dao.entity.PayTrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 支付宝Page支付实现类
 *
 * @author chenhaizhuang
 */
@Service("alipayPage")
public class AlipayPagePayService extends AlipayPayService {

    @Autowired
    public void setAlipayPayProperties(AlipayPayProperties alipayPayProperties) {
        this.alipayPayProperties = alipayPayProperties;
    }

    @Autowired
    public void setNotifyUrlProperties(NotifyUrlProperties notifyUrlProperties) {
        this.notifyUrlProperties = notifyUrlProperties;
    }

    @Override
    public String prepay(PayTrade payTrade) {
        String in = payTrade.getIn();
        AlipayClient alipayClient = AlipayPayFactory.getAlipayClient(alipayPayProperties, in);

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        model.setTotalAmount(payTrade.getAmount().toString());
        model.setSubject(payTrade.getDescription());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTimeExpire(payTrade.getExpireTime().format(DatePattern.NORM_DATETIME_FORMATTER));
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrlProperties.getPayNotifyUrl(in));
        request.setBizModel(model);

        AlipayTradePagePayResponse response;
        try {
            response = alipayClient.pageExecute(request);
        } catch (Exception e) {
            throw new RuntimeException("支付宝电脑网站支付 预支付失败", e);
        }
        Assert.isTrue(response.isSuccess(), "支付宝电脑网站支付 预支付失败：" + response.getSubMsg());
        return response.getBody();
    }
}
