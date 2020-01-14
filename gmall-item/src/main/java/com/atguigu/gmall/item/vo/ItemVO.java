package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVO {

    private Long skuId;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private Long spuId;
    private String spuName;

    private String skuTitle;
    private String skuSubTitle;
    private BigDecimal price;
    private BigDecimal weight;
    private Boolean store; // 库存信息

    private List<SkuImagesEntity> images;
    private List<ItemSaleVO> sales; // 促销信息

    private List<SkuSaleAttrValueEntity> saleAttrValues; // spu下的所有sku的销售组合

    private List<String> desc;

    private List<ItemGroupVO> groupVOS;
}
