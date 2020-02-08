package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVO {

    private String orderToken; // 防重

    private MemberReceiveAddressEntity address; // 收货人信息

    private Integer payType; // 支付方式

    private String deliveryCompany; // 物流公司（配送方式）

    private List<OrderItemVO> items; // 送货清单

    private Integer bounds; // 消费积分

    private BigDecimal totalPrice; // 总价，验价
}
