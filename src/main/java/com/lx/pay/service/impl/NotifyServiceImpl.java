package com.lx.pay.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lx.pay.constant.RedisKeyConst;
import com.lx.pay.core.IPayService;
import com.lx.pay.core.TradeResult;
import com.lx.pay.dao.PayTradeDAO;
import com.lx.pay.dao.RefundTradeDAO;
import com.lx.pay.dao.entity.PayTrade;
import com.lx.pay.dao.entity.RefundTrade;
import com.lx.pay.enums.PayStatusEnum;
import com.lx.pay.enums.RefundStatusEnum;
import com.lx.pay.exception.CustomizeException;
import com.lx.pay.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author chenhaizhuang
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    @Autowired
    private PayTradeDAO payTradeDAO;

    @Autowired
    private RefundTradeDAO refundTradeDAO;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IPayService payService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    @Qualifier("notifyExecutor")
    private Executor notifyExecutor;

    @Override
    public String payNotify(HttpServletRequest request, String in) {
        // 解析支付通知
        TradeResult tradeResult = payService.parsePayNotify(request, in);
        log.info("解析支付通知：{}", tradeResult);

        // 异步执行处理
        CompletableFuture.runAsync(() -> handlePayNotify(tradeResult), notifyExecutor)
                .exceptionally(e -> {
                    log.error("处理支付通知失败", e);
                    return null;
                });

        // 返回通知结果
        return tradeResult.getDefaultNotifyResult();
    }

    /**
     * 处理支付通知
     *
     * @param tradeResult 交易结果
     */
    private void handlePayNotify(TradeResult tradeResult) {
        String tradeNo = tradeResult.getTradeNo();
        String key = StrUtil.format(RedisKeyConst.LOCK_PAY, tradeNo);
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        PayTrade payTrade;
        try {
            // 锁单循环，避免其它操作冲突
            do {
                locked = lock.tryLock(100L, TimeUnit.MILLISECONDS);
                payTrade = payTradeDAO.getOne(Wrappers.<PayTrade>lambdaQuery()
                        .eq(PayTrade::getTradeNo, tradeNo));
                Assert.notNull(payTrade, "支付交易不存在");
                if (!PayStatusEnum.NOT_PAY.valueEquals(payTrade.getStatus())) {
                    return;
                }
            } while (!locked);

            // 更新支付交易
            payTrade.setOutTradeNo(tradeResult.getOutTradeNo());
            payTrade.setStatus(tradeResult.getStatus());
            payTrade.setFinishTime(tradeResult.getFinishTime());
            payTradeDAO.updateById(payTrade);

            // 通知业务方支付结果
            String msg = JSON.toJSONString(tradeResult);
            kafkaTemplate.send(payTrade.getBizMqTopic(), msg);
        } catch (InterruptedException e) {
            throw new CustomizeException("处理支付通知获取锁失败", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public String refundNotify(HttpServletRequest request, String in) {
        // 解析退款通知
        TradeResult tradeResult = payService.parseRefundNotify(request, in);
        log.info("解析退款通知：{}", tradeResult);

        // 异步执行处理
        CompletableFuture.runAsync(() -> handleRefundNotify(tradeResult), notifyExecutor)
                .exceptionally(e -> {
                    log.error("处理退款通知失败", e);
                    return null;
                });

        // 返回通知数据
        return tradeResult.getDefaultNotifyResult();
    }

    /**
     * 处理退款通知
     *
     * @param tradeResult 交易结果
     */
    @SuppressWarnings("all")
    private void handleRefundNotify(TradeResult tradeResult) {
        String tradeNo = tradeResult.getTradeNo();
        String key = StrUtil.format(RedisKeyConst.LOCK_REFUND_QUERY, tradeNo);
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        RefundTrade refundTrade;
        try {
            // 锁单循环，避免其它操作冲突
            do {
                locked = lock.tryLock(100L, TimeUnit.MILLISECONDS);
                refundTrade = refundTradeDAO.getOne(Wrappers.<RefundTrade>lambdaQuery()
                        .eq(RefundTrade::getTradeNo, tradeNo));
                Assert.notNull(refundTrade, "退款交易不存在");
                if (!RefundStatusEnum.IN_REFUND.valueEquals(refundTrade.getStatus())) {
                    return;
                }
            } while (!locked);

            // 更新退款交易
            refundTrade.setOutTradeNo(tradeResult.getOutTradeNo());
            refundTrade.setStatus(tradeResult.getStatus());
            refundTrade.setFinishTime(tradeResult.getFinishTime());
            refundTradeDAO.updateById(refundTrade);
        } catch (InterruptedException e) {
            throw new CustomizeException("处理退款通知获取锁失败", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
