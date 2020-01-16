package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.exception.UmsException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {

        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1: wrapper.eq("username", data); break;
            case 2: wrapper.eq("mobile", data); break;
            case 3: wrapper.eq("email", data); break;
            default:
                return null;
        }
        return this.count(wrapper) == 0;
    }

    @Override
    public void register(MemberEntity memberEntity, String code) {
        // 1. 校验验证码是否正确
        String redisCode = this.redisTemplate.opsForValue().get(memberEntity.getMobile());
        if (!StringUtils.equals(redisCode, code)) {
            throw new UmsException("用户验证码错误！");
        }
        // 2. 生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        memberEntity.setSalt(salt);

        // 3. 加盐加密
        memberEntity.setPassword(DigestUtils.md5Hex(memberEntity.getPassword() + salt));

        // 4. 保存用户信息
        memberEntity.setLevelId(1l);
        memberEntity.setSourceType(1);
        memberEntity.setIntegration(1000);
        memberEntity.setGrowth(1000);
        memberEntity.setStatus(1);
        memberEntity.setCreateTime(new Date());
        this.save(memberEntity);

        // 5. 删除验证码
        this.redisTemplate.delete(memberEntity.getMobile());
    }

    @Override
    public MemberEntity queryUser(String username, String password) {

        // 1. 根据用户名查询用户信息
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));

        // 2. 判断用户是否为空
        if (memberEntity == null) {
            return memberEntity;
        }

        // 3. 获取盐对用户输入的密码加盐加密
        password = DigestUtils.md5Hex(password + memberEntity.getSalt());

        // 4. 用户输入的密码和数据库中的密码进行比较
        if (!StringUtils.equals(password, memberEntity.getPassword())) {
            return null;
        }

        return memberEntity;
    }

}