package com.lx.pay.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 入口配置
 *
 * @author chenhaizhuang
 */
@Data
@Component
@ConfigurationProperties("pay")
@RefreshScope
public class InProperties {

    /**
     * 入口 -> 支付核心接口实现类Bean名
     */
    private Map<String, String> ins;
}
