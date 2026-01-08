-- 1. 初始化数据库
DROP DATABASE IF EXISTS `tiny_ai_interview`;

CREATE DATABASE `tiny_ai_interview`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `tiny_ai_interview`;

CREATE DATABASE `tiny_ai_interview` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 1. 用户在线状态表
CREATE TABLE `user_online_status` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                      `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                      `online_status` int(11) DEFAULT '0' COMMENT '1在线 0离线',
                                      `websocket_session_id` varchar(255) DEFAULT NULL,
                                      `last_active_time` datetime DEFAULT NULL,
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 聊天记录表
CREATE TABLE `chat_message` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                `sender_id` bigint(20) NOT NULL,
                                `receiver_id` bigint(20) NOT NULL,
                                `content` varchar(1024) DEFAULT NULL,
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 给聊天记录表增加“发送者类型”和“接收者类型”
-- 1=求职者, 2=招聘者(HR)
ALTER TABLE `chat_message`
    ADD COLUMN `sender_type` int(11) NOT NULL DEFAULT 1 COMMENT '1求职者 2招聘者' AFTER `sender_id`,
    ADD COLUMN `receiver_type` int(11) NOT NULL DEFAULT 1 COMMENT '1求职者 2招聘者' AFTER `receiver_id`;