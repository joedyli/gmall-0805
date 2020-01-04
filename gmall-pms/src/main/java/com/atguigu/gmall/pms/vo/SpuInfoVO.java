package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuInfoVO extends SpuInfoEntity {

    private List<String> spuImages; // spu描述信息（图片）

    private List<BaseAttrValueVO> baseAttrs; // 通用的规格属性

    private List<SkuInfoVO> skus;
}
