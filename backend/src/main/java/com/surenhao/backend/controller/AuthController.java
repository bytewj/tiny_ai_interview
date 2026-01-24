package com.surenhao.backend.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.surenhao.backend.annotation.Public;
import com.surenhao.backend.common.Result;
import com.surenhao.backend.entity.SysUser;
import com.surenhao.backend.entity.LoginUser; // 引入新类
import com.surenhao.backend.mapper.SysUserMapper;
import com.surenhao.backend.utils.UserContext;
import org.springframework.beans.BeanUtils;
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
@CrossOrigin
public class AuthController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Public
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        // 1. 校验账号密码 (查数据库完整实体)
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null || !user.getPassword().equals(password)) {
            return Result.error("账号或密码错误");
        }

        // 2. 【核心修改】转换为 LoginUser，剔除密码
        LoginUser loginUser = new LoginUser();
        // 使用 Spring 自带工具复制属性 (属性名相同会自动复制：id, username, nickname, avatar, role)
        BeanUtils.copyProperties(user, loginUser);

        // 3. 生成 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 4. 存入 Redis (只存精简后的 LoginUser JSON)
        String key = "login:token:" + token;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(loginUser), 30, TimeUnit.MINUTES);

        // 5. 返回结果 (返回给前端的也可以是这个精简对象)
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", loginUser); // 前端也不需要知道密码
        return Result.data(data);
    }

    @GetMapping("/contacts")
    public Result<List<SysUser>> getContacts() {
        // 从 ThreadLocal 获取当前用户 ID
        Long myId = UserContext.get().getId();

        // 查询其他人
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getId, myId));

        return Result.data(users);
    }

    @GetMapping("/info")
    public Result<LoginUser> getInfo() {
        // 直接返回 ThreadLocal 中的精简信息
        return Result.data(UserContext.get());
    }
}