package com.lx.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款交易
 *
 * @author chenhaizhuang
 */
@Data
@TableName("refund_trade")
public class RefundTrade {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 交易编号
     */
    private String tradeNo;

    /**
     * 外部交易编号
     */
    private String outTradeNo;

    /**
     * 退款金额
     */
    private BigDecimal amount;

    /**
     * 退款描述
     */
    private String description;

    /**
     * 退款状态：0-退款中，1-退款成功，2-退款失败
     */
    private Integer status;

    /**
     * 完成时间
     */
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
     * 支付交易编号
     */
    private String payTradeNo;

    /**
     * 支付交易
     */
    @TableField(exist = false)
    private PayTrade payTrade;
}
