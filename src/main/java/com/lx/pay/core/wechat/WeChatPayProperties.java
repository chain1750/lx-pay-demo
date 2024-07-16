package com.lx.pay.core.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 微信支付配置
 *
 * @author chenhaizhuang
 */
@Data
@Component
@ConfigurationProperties("pay.wechat")
@RefreshScope
public class WeChatPayProperties {

    /**
     * 入口 -> 微信appId
     */
    private Map<String, String> ins;

    /**
     * 商户
     */
    private List<Merchant> merchants;

    @Data
    public static class Merchant {

        /**
         * 商户号
         */
        private String merchantId;

        /**
         * 私钥路径
         */
        private String privateKeyPath;

        /**
         * 证书序号
         */
        private String serialNumber;

        /**
         * API V3 Key
         */
        private String apiV3Key;

        /**
         * 应用ID列表
         */
        private List<String> appIds;
    }
}
