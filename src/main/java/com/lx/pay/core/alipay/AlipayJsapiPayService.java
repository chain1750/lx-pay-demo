package com.lx.pay.core.alipay;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Assert;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeCreateModel;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.response.AlipayTradeCreateResponse;
import com.lx.pay.core.NotifyUrlProperties;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.exception.CustomizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 支付宝JSAPI支付实现类
 *
 * @author chenhaizhuang
 */
@Service("alipayJSAPI")
public class AlipayJsapiPayService extends AlipayPayService {

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

        AlipayTradeCreateModel model = new AlipayTradeCreateModel();
        model.setOutTradeNo(payTrade.getTradeNo());
        model.setTotalAmount(payTrade.getAmount().toString());
        model.setSubject(payTrade.getDescription());
        model.setProductCode("JSAPI_PAY");
        model.setBuyerOpenId(getOpenId(payTrade));
        model.setOpAppId(alipayPayProperties.getIns().get(in));
        model.setTimeExpire(payTrade.getExpireTime().format(DatePattern.NORM_DATETIME_FORMATTER));
        AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
        request.setNotifyUrl(notifyUrlProperties.getPayNotifyUrl(in));
        request.setBizModel(model);

        AlipayTradeCreateResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (Exception e) {
            throw new CustomizeException("支付宝小程序支付 预支付失败", e);
        }
        Assert.isTrue(response.isSuccess(), "支付宝小程序支付 预支付失败：" + response.getSubMsg());
        return response.getBody();
    }

    private String getOpenId(PayTrade payTrade) {
        // 用户经过小程序登录后，在用户服务上保存了用户的openId
        // 在此可通过入口、用户ID等信息获取到对于的openId
        return "";
    }
}
