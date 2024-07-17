package com.lx.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.lx.pay.model.req.QueryPayReq;
import com.lx.pay.model.resp.QueryPayResp;
import com.lx.pay.service.BizService;
import com.lx.pay.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author chenhaizhuang
 */
@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

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
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Autowired
    private BizService bizService;

    @Override
    public void handleNotPay() {
        // 查询未支付的交易
        List<PayTrade> payTrades = payTradeDAO.list(Wrappers.<PayTrade>lambdaQuery()
                .select(PayTrade::getTradeNo)
                .eq(PayTrade::getStatus, PayStatusEnum.NOT_PAY.getValue()));
        log.info("处理未支付数量：{}", payTrades.size());

        // 异步执行处理
        payTrades.forEach(payTrade ->
                CompletableFuture.runAsync(() -> {
                            QueryPayReq req = new QueryPayReq();
                            req.setTradeNo(payTrade.getTradeNo());
                            QueryPayResp queryPayResp = bizService.queryPay(req);

                            // 通知业务方支付结果
                            String msg = JSON.toJSONString(BeanUtil.copyProperties(queryPayResp, TradeResult.class));
                            kafkaTemplate.send(payTrade.getBizMqTopic(), msg);
                        }, taskExecutor)
                        .exceptionally(e -> {
                            log.error("处理未支付失败", e);
                            return null;
                        })
        );
    }

    @Override
    public void handleInRefund() {
        // 查询退款中的交易
        List<RefundTrade> refundTrades = refundTradeDAO.list(Wrappers.<RefundTrade>lambdaQuery()
                .select(RefundTrade::getTradeNo)
                .eq(RefundTrade::getStatus, RefundStatusEnum.IN_REFUND.getValue()));
        log.info("处理退款中数量：{}", refundTrades.size());

        // 异步执行处理
        refundTrades.forEach(refundTrade ->
                CompletableFuture.runAsync(() -> handleInRefundTask(refundTrade), taskExecutor)
                        .exceptionally(e -> {
                            log.error("处理退款中失败", e);
                            return null;
                        })
        );
    }

    /**
     * 处理退款中任务
     *
     * @param refundTrade 退款交易
     */
    @SuppressWarnings("all")
    private void handleInRefundTask(RefundTrade refundTrade) {
        String tradeNo = refundTrade.getTradeNo();
        String key = StrUtil.format(RedisKeyConst.LOCK_REFUND_QUERY, tradeNo);
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
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

            // 查询支付交易
            PayTrade payTrade = payTradeDAO.getOne(Wrappers.<PayTrade>lambdaQuery()
                    .eq(PayTrade::getTradeNo, refundTrade.getPayTradeNo()));
            Assert.notNull(payTrade, "支付交易不存在");

            // 查询退款
            refundTrade.setPayTrade(payTrade);
            TradeResult tradeResult = payService.queryRefund(refundTrade);

            // 更新退款交易
            refundTrade.setOutTradeNo(tradeResult.getOutTradeNo());
            refundTrade.setStatus(tradeResult.getStatus());
            refundTrade.setFinishTime(tradeResult.getFinishTime());
            refundTradeDAO.updateById(refundTrade);
        } catch (InterruptedException e) {
            throw new CustomizeException("处理退款任务获取锁失败", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
