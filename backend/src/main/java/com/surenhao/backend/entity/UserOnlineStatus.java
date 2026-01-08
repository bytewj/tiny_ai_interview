package com.surenhao.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("user_online_status")
public class UserOnlineStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer onlineStatus;
    private String websocketSessionId;
    private Date lastActiveTime;
}