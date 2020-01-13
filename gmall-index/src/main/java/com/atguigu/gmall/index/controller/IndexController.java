package com.atguigu.gmall.index.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("index")
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("cates")
    public Resp<List<CategoryEntity>> queryLvl1Categories(){
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl1Categories();
        return Resp.ok(categoryEntities);
    }

    @GetMapping("cates/{pid}")
    public Resp<List<CategoryVO>> queryCategoriesWithSub(@PathVariable("pid")Long pid){
        List<CategoryVO> categoryVOS = indexService.queryCategoriesWithSub(pid);
        return Resp.ok(categoryVOS);
    }

    @GetMapping("test/lock")
    public Resp<Object> testLock(){
        this.indexService.testLock();
        return Resp.ok(null);
    }

    @GetMapping("test/read")
    public Resp<String> testRead(){
        String msg = this.indexService.testRead();
        return Resp.ok(msg);
    }

    @GetMapping("test/write")
    public Resp<String> testWrite(){
        String msg = this.indexService.testWrite();
        return Resp.ok(msg);
    }

    @GetMapping("test/latch")
    public Resp<String> testLatch() throws InterruptedException {
        String msg = this.indexService.testLatch();
        return Resp.ok(msg);
    }

    @GetMapping("test/countdown")
    public Resp<String> testCountDown(){
        String msg = this.indexService.testCountDown();
        return Resp.ok(msg);
    }


}
