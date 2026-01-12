package com.surenhao.backend.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.surenhao.backend.annotation.Public; // 引入自定义注解
import com.surenhao.backend.common.Result;     // 引入自定义返回结果
import com.surenhao.backend.entity.SysUser;
import com.surenhao.backend.mapper.SysUserMapper;
import com.surenhao.backend.utils.UserContext; // 引入 ThreadLocal 工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin // 允许跨域
public class AuthController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 1. 登录接口
     * 必须加 @Public，否则会被 AOP 拦截导致无法登录
     */
    @Public
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        // 1. 校验账号密码
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null || !user.getPassword().equals(password)) {
            return Result.error("账号或密码错误");
        }

        // 2. 生成 Token (UUID)
        String token = UUID.randomUUID().toString().replace("-", "");

        // 3. 存入 Redis (Key: login:token:xxx, Value: UserJSON, Expire: 30min)
        String key = "login:token:" + token;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(user), 30, TimeUnit.MINUTES);

        // 4. 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.data(data);
    }

    /**
     * 2. 获取联系人列表
     * 未加 @Public -> 默认需要登录
     */
    @GetMapping("/contacts")
    public Result<List<SysUser>> getContacts() {
        // 从 ThreadLocal 获取当前用户 ID，无需再查数据库验证 Token
        Long myId = UserContext.get().getId();

        // 查询除了我之外的所有用户
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getId, myId));

        return Result.data(users);
    }

    /**
     * 3. 获取当前用户信息 (用于前端自动登录/刷新)
     * 未加 @Public -> 默认需要登录
     */
    @GetMapping("/info")
    public Result<SysUser> getInfo() {
        // 直接返回 ThreadLocal 中的用户信息，性能极高
        // 因为请求经过拦截器时，已经把 Redis 里的用户信息放进 ThreadLocal 了
        return Result.data(UserContext.get());
    }
}