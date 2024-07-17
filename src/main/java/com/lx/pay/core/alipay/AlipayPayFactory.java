package com.lx.pay.core.alipay;

import cn.hutool.core.lang.Assert;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付宝支付工厂
 *
 * @author chenhaizhuang
 */
public class AlipayPayFactory {

    private static final Map<String, AlipayClient> ALIPAY_CLIENT = new ConcurrentHashMap<>();

    public static synchronized AlipayPayProperties.Account getAccount(AlipayPayProperties alipayPayProperties,
                                                                      String in) {
        Assert.isTrue(alipayPayProperties.getIns().containsKey(in), "入口没有可用的支付方式");
        String appId = alipayPayProperties.getIns().get(in);
        Optional<AlipayPayProperties.Account> optionalAccount = alipayPayProperties.getAccounts().stream()
                .filter(e -> e.getApps().containsKey(appId))
                .findFirst();
        return optionalAccount.orElseThrow(() -> new RuntimeException("入口没有可用的支付方式"));
    }

    public static synchronized AlipayClient getAlipayClient(AlipayPayProperties alipayPayProperties,
                                                            String in) {
        String appId = alipayPayProperties.getIns().get(in);
        if (ALIPAY_CLIENT.containsKey(appId)) {
            return ALIPAY_CLIENT.get(appId);
        }
        AlipayPayProperties.Account account = getAccount(alipayPayProperties, in);

        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(alipayPayProperties.getServerUrl());
        alipayConfig.setAppId(appId);
        alipayConfig.setPrivateKey(account.getApps().get(appId));
        alipayConfig.setAlipayPublicKey(account.getPublicKey());
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
            ALIPAY_CLIENT.put(appId, alipayClient);
            return alipayClient;
        } catch (Exception e) {
            throw new RuntimeException("创建支付宝客户端失败", e);
        }
    }
}
