package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVO {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs; // 销售属性
    private BigDecimal price;
    private Integer count;
    private Boolean store = false; // 库存
    private List<ItemSaleVO> sales; // 促销信息

    private BigDecimal weight;
}
