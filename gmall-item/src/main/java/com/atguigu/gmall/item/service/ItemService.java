package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVO queryItemVO(Long skuId) {

        ItemVO itemVO = new ItemVO();

        itemVO.setSkuId(skuId);
        // 根据skuId查询sku
        CompletableFuture<SkuInfoEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return null;
            }
            itemVO.setWeight(skuInfoEntity.getWeight());
            itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVO.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVO.setPrice(skuInfoEntity.getPrice());
            return skuInfoEntity;
        }, threadPoolExecutor);

        CompletableFuture<Void> categroyCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            // 根据sku中的categoryId查询分类
            Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {
                itemVO.setCategoryId(categoryEntity.getCatId());
                itemVO.setCategoryName(categoryEntity.getName());
            }
        }, threadPoolExecutor);


        // 根据sku中的brandId查询品牌
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuInfoEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if (brandEntity != null) {
                itemVO.setBrandId(brandEntity.getBrandId());
                itemVO.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);
        // 根据sku中的spuId查询spu
        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = this.pmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVO.setSpuId(spuInfoEntity.getId());
                itemVO.setSpuName(spuInfoEntity.getSpuName());
            }
        }, threadPoolExecutor);

        // 根据skuId查询图片
        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> imagesResp = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResp.getData();
            itemVO.setImages(skuImagesEntities);
        }, threadPoolExecutor);

        // 根据skuId查询库存信息
        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            // 根据skuId查询营销信息：积分 打折 满减
            Resp<List<ItemSaleVO>> itemSalesResp = this.smsClient.queryItemSaleVOBySkuId(skuId);
            List<ItemSaleVO> itemSaleVOS = itemSalesResp.getData();
            itemVO.setSales(itemSaleVOS);
        }, threadPoolExecutor);

        // 根据sku中的spuId查询描述信息
        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.pmsClient.querySpuDescBySpuId(skuInfoEntity.getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
            if (spuInfoDescEntity != null && StringUtils.isNotBlank(spuInfoDescEntity.getDecript())) {
                itemVO.setDesc(Arrays.asList(StringUtils.split(spuInfoDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        // 1.根据sku中的categoryId查询分组
        // 2.遍历组到中间表中查询每个组的规格参数id
        // 3.根据spuId和attrId查询规格参数名及值
        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<ItemGroupVO>> groupResp = this.pmsClient.queryItemGroupVOsByCidAndSpuId(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            List<ItemGroupVO> itemGroupVOS = groupResp.getData();
            itemVO.setGroupVOS(itemGroupVOS);
        }, threadPoolExecutor);

        // 1.根据sku中的spuId查询skus
        // 2.根据skus获取skuIds
        // 3.根据skuIds查询销售属性
        CompletableFuture<Void> attrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<SkuSaleAttrValueEntity>> skuSaleAttrValueResp = this.pmsClient.querySaleAttrValueBySpuId(skuInfoEntity.getSpuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = skuSaleAttrValueResp.getData();
            itemVO.setSaleAttrValues(skuSaleAttrValueEntities);
        }, threadPoolExecutor);

        CompletableFuture.allOf(categroyCompletableFuture, brandCompletableFuture, spuCompletableFuture, imageCompletableFuture,
                storeCompletableFuture, saleCompletableFuture, descCompletableFuture, groupCompletableFuture, attrCompletableFuture).join();

        return itemVO;
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        CompletableFuture<String> completedFuture1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("开启一个任务！");
            return "supplyAsync";
        }).thenCombineAsync(CompletableFuture.completedFuture("completedFuture"), (t, u) -> {
            System.out.println(t + "\n" + u);
            return "thenCombineAsync";
        }).thenApplyAsync(t -> {
            System.out.println(t);
            return "hello";
        });

        CompletableFuture<Void> completableFuture2 = CompletableFuture.runAsync(() -> {
            System.out.println("这是第二个任务");
        });
        CompletableFuture<Void> completableFuture3 = CompletableFuture.runAsync(() -> {
            System.out.println("这是第三个任务");
        });
        CompletableFuture<Void> completableFuture4 = CompletableFuture.runAsync(() -> {
            System.out.println("这是第四个任务");
        });

        CompletableFuture.allOf(completedFuture1, completableFuture2, completableFuture3, completableFuture4).get();
        System.out.println("这是主方法！");

        // 开启另一个子任务，不需要获取这个任务的返回值
//        CompletableFuture.runAsync(() -> {
//            System.out.println("开启一个不带返回值的子任务");
//        });

        // 开启另一个子任务，需要获取这个任务的返回值
//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("开启一个带返回值的子任务");
////            int i = 1 / 0;
//            return "hello";
//        }).thenApplyAsync(t -> {
//            System.out.println("上一个任务的返回结果：" + t);
//            return "thenApplyAsync";
//        }).thenAcceptAsync(t -> {
//            System.out.println("thenAcceptAsync上一个任务的返回结果：" + t);
//        }).whenCompleteAsync((t, u) -> { // 处理正常结果集，处理异常结果集  返回值：t-上一个任务正常返回值 u-上一个任务的异常信息
//            System.out.println("t: " + t);
//            System.out.println("u: " + u);
//        }).handleAsync((t, u) -> {
//            System.out.println("handle t: " + t);
//            System.out.println("handle u: " + u);
//            return "handle";
//        });

//        new MyThread().start();
//        new Thread(new MyRunnable()).start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                System.out.println("这是一个子线程方法！Runnable接口，匿名内部类");
//            }
//        }).start();

//        new Thread(() -> {
//            System.out.println("这是一个子线程方法！Runnable接口，lambda表达式");
//        }).start();

//        FutureTask<String> futureTask = new FutureTask<>(new MyCallable());
//        new Thread(futureTask).start();
//        try {
//            System.out.println(futureTask.get());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }

//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        for (int i = 0; i < 10; i++) {
//            executorService.execute(() -> {
//                System.out.println("线程池开启一个子任务：" + Thread.currentThread().getName());
//            });
//        }
//        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(2);
//        threadPool.schedule(() -> {
//            System.out.println("线程池的定时任务：" + Thread.currentThread().getName());
//        }, 20, TimeUnit.SECONDS);
//        threadPool.scheduleAtFixedRate(() -> {
//            System.out.println("线程池的定时任务：" + Thread.currentThread().getName());
//        }, 5, 10, TimeUnit.SECONDS);

//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 5, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
//
//        for (int i = 0; i < 500; i++) {
//            threadPoolExecutor.execute(() -> {
//                System.out.println("自定义线程池执行任务！" + Thread.currentThread().getName());
//            });
//        }
//
//        System.out.println("这是主方法！");
    }
}

class MyCallable implements Callable<String> {

    @Override
    public String call() throws Exception {
        System.out.println("这也是一个子线程方法！Callable");
        return "hello callable!";
    }
}

class MyRunnable implements Runnable{
    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("这是一个子线程方法！Runnable");
    }
}

class MyThread extends Thread{
    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("这是一个子线程方法！");
    }
}
