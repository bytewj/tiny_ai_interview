package com.surenhao.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.surenhao.backend.entity.SysUser;
import com.surenhao.backend.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private SysUserMapper userMapper;

    // 1. 登录接口 (保持不变)
    @PostMapping("/login")
    public SaResult login(@RequestBody Map<String, String> params) {
        // ... (保持你之前的代码不变) ...
        // 记得要有一句: StpUtil.login(user.getId());
        // ...
        String username = params.get("username");
        String password = params.get("password");
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !user.getPassword().equals(password)) return SaResult.error("账号错误");

        StpUtil.login(user.getId()); // 登录

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("user", user);
        return SaResult.data(data);
    }

    // 2. 获取联系人 (保持不变)
    @GetMapping("/contacts")
    public SaResult getContacts(@RequestParam Long myId) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().ne(SysUser::getId, myId));
        return SaResult.data(users);
    }

    // ✅✅✅ 新增：获取当前用户信息 (用于自动登录)
    @GetMapping("/info")
    public SaResult getInfo() {
        // 因为加了拦截器，能进来的肯定是登录过的
        long loginId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(loginId);
        return SaResult.data(user);
    }
}