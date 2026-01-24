package com.surenhao.backend.controller;

import com.surenhao.backend.annotation.RequireRole;
import com.surenhao.backend.common.Result;
import com.surenhao.backend.common.RoleEnum;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * 场景：封禁用户
     * 只有管理员能调这个接口
     */
    @RequireRole(RoleEnum.ADMIN) // <--- 加上这一行，AOP 就会生效
    @PostMapping("/banUser/{userId}")
    public Result<String> banUser(@PathVariable Long userId) {
        // 这里的代码只管业务逻辑，不用管你是谁，能进来说明肯定是管理员
        System.out.println("执行封禁逻辑，封禁用户ID: " + userId);

        // userService.ban(userId);

        return Result.success("封禁成功");
    }
}