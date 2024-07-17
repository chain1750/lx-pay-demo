package com.lx.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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
import com.lx.pay.model.req.ClosePayReq;
import com.lx.pay.model.req.PrepayReq;
import com.lx.pay.model.req.QueryPayReq;
import com.lx.pay.model.req.RefundReq;
import com.lx.pay.model.resp.PrepayResp;
import com.lx.pay.model.resp.QueryPayResp;
import com.lx.pay.model.resp.RefundResp;
import com.lx.pay.service.BizService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author chenhaizhuang
 */
@Slf4j
@Service
public class BizServiceImpl implements BizService {

    @Autowired
    private PayTradeDAO payTradeDAO;

    @Autowired
    private RefundTradeDAO refundTradeDAO;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IPayService payService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrepayResp prepay(PrepayReq req) {
        String key = StrUtil.format(RedisKeyConst.LOCK_PREPAY, req.getBiz(), req.getBizDataId());
        RLock lock = redissonClient.getLock(key);
        Assert.isTrue(lock.tryLock(), "操作频繁，请稍后再试");
        try {
            // 关闭未支付
            closeNotPay(req);

            LocalDateTime now = LocalDateTime.now();
            Assert.isTrue(now.isBefore(req.getExpireTime()), "支付过期时间必须大于当前时间");

            String tradeNo = "10001" + now.format(DatePattern.PURE_DATE_FORMATTER) + IdUtil.getSnowflakeNextIdStr();

            // 创建支付交易
            PayTrade payTrade = BeanUtil.copyProperties(req, PayTrade.class);
            payTrade.setTradeNo(tradeNo);
            payTrade.setStatus(PayStatusEnum.NOT_PAY.getValue());
            payTradeDAO.save(payTrade);
            log.info("创建支付交易：{}", payTrade);

            // 预支付
            String prepay = payService.prepay(payTrade);
            log.info("预支付：{}", prepay);

            // 预支付结果
            PrepayResp resp = new PrepayResp();
            resp.setTradeNo(payTrade.getTradeNo());
            resp.setPrepay(prepay);
            return resp;
        } finally {
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeNotPay(PrepayReq req) {
        // 查询该业务下的交易
        List<PayTrade> payTrades = payTradeDAO.list(Wrappers.<PayTrade>lambdaQuery()
                .eq(PayTrade::getBiz, req.getBiz())
                .eq(PayTrade::getBizDataId, req.getBizDataId()));
        if (CollUtil.isEmpty(payTrades)) {
            return;
        }
        /*
        如果存在已支付，则报错
        如果存在未支付，则关闭该支付
         */
        for (PayTrade payTrade : payTrades) {
            Assert.isTrue(!PayStatusEnum.PAY_SUCCESS.valueEquals(payTrade.getStatus()), "业务已完成支付");
            if (PayStatusEnum.PAY_CLOSED.valueEquals(payTrade.getStatus())) {
                continue;
            }
            ClosePayReq closePayReq = new ClosePayReq();
            closePayReq.setTradeNo(payTrade.getTradeNo());
            closePay(closePayReq);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePay(ClosePayReq req) {
        String tradeNo = req.getTradeNo();
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
                Assert.isTrue(PayStatusEnum.NOT_PAY.valueEquals(payTrade.getStatus()), "交易已支付/已关闭，无法关闭");
            } while (!locked);

            // 更新支付交易
            payTrade.setStatus(PayStatusEnum.PAY_CLOSED.getValue());
            payTradeDAO.updateById(payTrade);
            log.info("关闭支付更新支付交易：{}", payTrade);

            // 关闭支付
            payService.closePay(payTrade);
        } catch (InterruptedException e) {
            throw new CustomizeException("关闭支付获取锁失败", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueryPayResp queryPay(QueryPayReq req) {
        String tradeNo = req.getTradeNo();
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
                // 已支付/已关闭返回支付结果
                if (!PayStatusEnum.NOT_PAY.valueEquals(payTrade.getStatus())) {
                    return BeanUtil.copyProperties(payTrade, QueryPayResp.class);
                }
            } while (!locked);

            // 查询支付
            TradeResult tradeResult = payService.queryPay(payTrade);
            log.info("查询支付：{}", tradeResult);

            // 更新
            updatePay(tradeResult, payTrade);

            // 返回支付结果
            return BeanUtil.copyProperties(payTrade, QueryPayResp.class);
        } catch (InterruptedException e) {
            throw new CustomizeException("查询支付获取锁失败", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * 更新支付交易
     *
     * @param tradeResult 交易结果
     * @param payTrade    支付交易
     */
    private void updatePay(TradeResult tradeResult, PayTrade payTrade) {
        // 过期关闭支付
        Integer status = tradeResult.getStatus();
        if (PayStatusEnum.NOT_PAY.valueEquals(status) && LocalDateTime.now().isAfter(payTrade.getExpireTime())) {
            log.info("支付交易过期，关闭支付：{}", payTrade.getTradeNo());
            payService.closePay(payTrade);
            tradeResult.setStatus(PayStatusEnum.PAY_CLOSED.getValue());
        }

        // 未支付不更新
        status = tradeResult.getStatus();
        if (PayStatusEnum.NOT_PAY.valueEquals(status)) {
            return;
        }

        // 更新支付交易
        payTrade.setOutTradeNo(tradeResult.getOutTradeNo());
        payTrade.setStatus(status);
        payTrade.setFinishTime(tradeResult.getFinishTime());
        payTradeDAO.updateById(payTrade);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResp refund(RefundReq req) {
        String payTradeNo = req.getPayTradeNo();
        String key = StrUtil.format(RedisKeyConst.LOCK_REFUND_EXEC, payTradeNo);
        RLock lock = redissonClient.getLock(key);
        Assert.isTrue(lock.tryLock(), "操作频繁，请稍后再试");
        try {
            PayTrade payTrade = payTradeDAO.getOne(Wrappers.<PayTrade>lambdaQuery()
                    .eq(PayTrade::getTradeNo, payTradeNo));
            Assert.notNull(payTrade, "支付交易不存在");
            Assert.isTrue(PayStatusEnum.PAY_SUCCESS.valueEquals(payTrade.getStatus()), "交易未支付，无法退款");

            // 创建退款交易
            LocalDateTime now = LocalDateTime.now();
            String tradeNo = "10002" + now.format(DatePattern.PURE_DATE_FORMATTER) + IdUtil.getSnowflakeNextIdStr();
            RefundTrade refundTrade = BeanUtil.copyProperties(req, RefundTrade.class);
            refundTrade.setTradeNo(tradeNo);
            refundTrade.setStatus(RefundStatusEnum.IN_REFUND.getValue());
            refundTradeDAO.save(refundTrade);
            log.info("创建退款交易：{}", refundTrade);

            // 退款
            refundTrade.setPayTrade(payTrade);
            payService.refund(refundTrade);

            // 退款交易ID
            RefundResp resp = new RefundResp();
            resp.setTradeNo(refundTrade.getTradeNo());
            return resp;
        } finally {
            lock.unlock();
        }
    }
}
