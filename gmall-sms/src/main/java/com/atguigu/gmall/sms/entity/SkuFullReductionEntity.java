package com.atguigu.gmall.sms.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品满减信息
 * 
 * @author lixianfeng
 * @email lxf@atguigu.com
 * @date 2020-01-04 11:33:42
 */
@ApiModel
@Data
@TableName("sms_sku_full_reduction")
public class SkuFullReductionEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@ApiModelProperty(name = "id",value = "id")
	private Long id;
	/**
	 * spu_id
	 */
	@ApiModelProperty(name = "skuId",value = "spu_id")
	private Long skuId;
	/**
	 * 满多少
	 */
	@ApiModelProperty(name = "fullPrice",value = "满多少")
	private BigDecimal fullPrice;
	/**
	 * 减多少
	 */
	@ApiModelProperty(name = "reducePrice",value = "减多少")
	private BigDecimal reducePrice;
	/**
	 * 是否参与其他优惠
	 */
	@ApiModelProperty(name = "addOther",value = "是否参与其他优惠")
	private Integer addOther;

}
