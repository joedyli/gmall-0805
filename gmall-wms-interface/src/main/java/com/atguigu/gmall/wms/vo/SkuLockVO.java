package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVO {

    private String orderToken;
    private Long skuId;
    private Integer count;
    private Boolean lock = false; // 锁定状态，true-验库并锁库成功 false-库存不足
    private Long wareSkuId; // 如果锁定成功的情况下，需要记录锁定的仓库id
}
