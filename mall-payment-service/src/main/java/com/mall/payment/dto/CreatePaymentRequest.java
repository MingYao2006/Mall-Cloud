package com.mall.payment.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额最小0.01")
    private BigDecimal amount;

    @NotNull(message = "支付类型不能为空")
    private Integer paymentType;

    private Long orderId;
}