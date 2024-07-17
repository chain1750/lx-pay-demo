package com.lx.pay.core.alipay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支付宝支付配置
 *
 * @author chenhaizhuang
 */
@Data
@Component
@ConfigurationProperties("pay.alipay")
@RefreshScope
public class AlipayPayProperties {

    /**
     * 入口 -> 支付宝appId
     */
    private Map<String, String> ins;

    /**
     * 支付宝网关地址
     */
    private String serverUrl;

    /**
     * 账号
     */
    private List<Account> accounts;

    @Data
    public static class Account {

        /**
         * 商家ID
         */
        private String sellerId;

        /**
         * 支付宝公钥
         */
        private String publicKey;

        /**
         * appId -> 应用私钥
         */
        private Map<String, String> apps;
    }
}
