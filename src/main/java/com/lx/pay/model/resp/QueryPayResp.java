package com.lx.pay.model.resp;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 查询支付结果
 *
 * @author chenhaizhuang
 */
@Data
public class QueryPayResp {

    /**
     * 创建时间
     */
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_MS_PATTERN)
    private LocalDateTime createTime;

    /**
     * 交易编号
     */
    private String tradeNo;

    /**
     * 外部交易编号
     */
    private String outTradeNo;

    /**
     * 入口
     */
    private String in;

    /**
     * 用户IP
     */
    private String userIp;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付描述
     */
    private String description;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_MS_PATTERN)
    private LocalDateTime expireTime;

    /**
     * 支付状态：0-未支付，1-已支付，2-已关闭
     */
    private Integer status;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = DatePattern.NORM_DATETIME_MS_PATTERN)
    private LocalDateTime finishTime;

    /**
     * 业务方
     */
    private String biz;

    /**
     * 业务数据ID
     */
    private String bizDataId;

    /**
     * 业务附加数据
     */
    private String bizAttach;
}
