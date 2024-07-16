package com.lx.pay.core;

import cn.hutool.core.lang.Assert;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.dao.entity.RefundTrade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * 默认支付核心接口实现类
 *
 * @author chenhaizhuang
 */
@Slf4j
@Service
@Primary
public class DefaultPayService implements IPayService {

    @Autowired
    private InProperties inProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public String prepay(PayTrade payTrade) {
        return select(payTrade.getIn()).prepay(payTrade);
    }

    @Override
    public void closePay(PayTrade payTrade) {
        select(payTrade.getIn()).closePay(payTrade);
    }

    @Override
    public TradeResult queryPay(PayTrade payTrade) {
        return select(payTrade.getIn()).queryPay(payTrade);
    }

    @Override
    public TradeResult parsePayNotify(HttpServletRequest request, String in) {
        return select(in).parsePayNotify(request, in);
    }

    @Override
    public void refund(RefundTrade refundTrade) {
        select(refundTrade.getPayTrade().getIn()).refund(refundTrade);
    }

    @Override
    public TradeResult queryRefund(RefundTrade refundTrade) {
        return select(refundTrade.getPayTrade().getIn()).queryRefund(refundTrade);
    }

    @Override
    public TradeResult parseRefundNotify(HttpServletRequest request, String in) {
        return select(in).parseRefundNotify(request, in);
    }

    /**
     * 获取支付核心接口实现类
     *
     * @param in 入口
     * @return IPayService
     */
    private IPayService select(String in) {
        Assert.isTrue(inProperties.getIns().containsKey(in), "入口没有可用的支付方式");
        String beanName = inProperties.getIns().get(in);

        Assert.isTrue(applicationContext.containsBean(beanName), "入口没有可用的支付方式");
        return applicationContext.getBean(beanName, IPayService.class);
    }
}
