package com.lx.pay.core.alipay;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Assert;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.exception.CustomizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 支付宝APP支付实现类
 *
 * @author chenhaizhuang
 */
@Service("alipayApp")
public class AlipayAppPayService extends AlipayPayService {

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

        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        model.setTotalAmount(payTrade.getAmount().toString());
        model.setSubject(payTrade.getDescription());
        model.setTimeExpire(payTrade.getExpireTime().format(DatePattern.NORM_DATETIME_FORMATTER));
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setNotifyUrl(notifyUrlProperties.getPayNotifyUrl(in));
        request.setBizModel(model);

        AlipayTradeAppPayResponse response;
        try {
            response = alipayClient.sdkExecute(request);
        } catch (Exception e) {
            throw new CustomizeException("支付宝APP支付 预支付失败", e);
        }
        Assert.isTrue(response.isSuccess(), "支付宝APP支付 预支付失败：" + response.getSubMsg());
        return response.getBody();
    }
}
