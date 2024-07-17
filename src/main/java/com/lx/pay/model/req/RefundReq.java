package com.lx.pay.model.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 退款请求
 *
 * @author chenhaizhuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundReq {

    /**
     * 支付交易编号
     */
    @NotBlank(message = "支付交易编号不能为空")
    private String payTradeNo;

    /**
     * 退款金额，单位元，两位小数，不能大于支付金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "至少退款0.01元")
    private BigDecimal amount;

    /**
     * 退款描述，请简略描述，若需详细描述请由业务方存储
     */
    @NotBlank(message = "退款描述不能为空")
    private String description;

    /**
     * 业务方，业务方+业务数据ID唯一标识一次退款交易
     */
    @NotBlank(message = "业务方不能为空")
    private String biz;

    /**
     * 业务数据ID，对应业务方的唯一键，业务方+业务数据ID唯一标识一次退款交易
     */
    @NotBlank(message = "业务数据ID不能为空")
    private String bizDataId;
}
