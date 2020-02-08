package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "wms:stock:";

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<SkuLockVO> checkAndLock(List<SkuLockVO> skuLockVOS) {

        // 判断传递的数据是否为空
        if (CollectionUtils.isEmpty(skuLockVOS)) {
            return null;
        }

        // 遍历清单集合，验库存并锁库存
        skuLockVOS.forEach(skuLockVO -> {
            this.checkLock(skuLockVO);
        });

        // 判断锁定结果集中是否包含锁定失败的商品（如果有任何一个商品锁定失败，已经锁定成功的商品应该回滚）
        if (skuLockVOS.stream().anyMatch(skuLockVO -> skuLockVO.getLock() == false)){
            // 获取已经锁定成功商品
            skuLockVOS.stream().filter(skuLockVO -> skuLockVO.getLock()).forEach(skuLockVO -> {
                // 解锁库存
                this.wareSkuDao.unLock(skuLockVO.getWareSkuId(), skuLockVO.getCount());
            });

            return skuLockVOS;
        }
        // 把库存的锁定信息保存到redis中，方便获取锁定库存的信息
        String orderToken = skuLockVOS.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(skuLockVOS));

        return null;
    }

    /**
     * 验库存并锁库存
     * 为保证原子性必须添加分布式锁
     */
    private void checkLock(SkuLockVO skuLockVO){
        RLock fairLock = this.redissonClient.getFairLock("lock" + skuLockVO.getSkuId());
        fairLock.lock();

        // 验库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuDao.check(skuLockVO.getSkuId(), skuLockVO.getCount());
        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
            // 锁库存，大数据分析以就近的仓库锁库存，这里我们就取第一个仓库锁库存
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            int lock = this.wareSkuDao.lock(wareSkuEntity.getId(), skuLockVO.getCount());
            if (lock != 0) {
                skuLockVO.setLock(true);
                skuLockVO.setWareSkuId(wareSkuEntity.getId());
            }
        }

        fairLock.unlock();
    }

}