package com.lx.pay.core;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 通知地址配置
 *
 * @author chenhaizhuang
 */
@Data
@Component
@ConfigurationProperties("pay.notify-url")
@RefreshScope
public class NotifyUrlProperties {

    /**
     * 支付地址
     */
    private String pay;

    /**
     * 退款地址
     */
    private String refund;

    public String getPayNotifyUrl(String in) {
        return StrUtil.format(pay, in);
    }

    public String getRefundNotifyUrl(String in) {
        return StrUtil.format(refund, in);
    }
}
