/*
Navicat MySQL Data Transfer
Source Database       : webconfig_trace

Target Server Type    : MYSQL
Target Server Version : 50734
File Encoding         : 65001

Date: 2024-03-05 14:33:47
*/

SET FOREIGN_KEY_CHECKS=0;

USE webconfig_trace;

-- ----------------------------
-- Table structure for trace_walking_compete
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trace_walking_compete` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` varchar(50) NOT NULL COMMENT '链路的唯一标识',
  `request_url` varchar(200) DEFAULT NULL COMMENT '链路方法的请求入口的URL',
  `trace_start_time` datetime(3) DEFAULT NULL COMMENT '链路开始的时间',
  `trace_end_time` datetime(3) DEFAULT NULL COMMENT '链路的结束时间',
  `trace_time_consume` int(11) DEFAULT '0' COMMENT '链路的耗时（单位：毫秒）',
  `application_name` varchar(100) DEFAULT NULL COMMENT '链路所在服务的名称',
  `trace_start_pos` varchar(500) DEFAULT NULL COMMENT '链路的起点',
  `trace_end_pos` varchar(500) DEFAULT NULL COMMENT '链路的终点',
  `trace_sum` int(11) DEFAULT '0' COMMENT '链路数',
  `invoke_method_sum` int(11) DEFAULT '0' COMMENT '调用的方法数（包含异常方法）',
  `exception_flag` tinyint(1) DEFAULT '0' COMMENT '是否有异常（0：否 1：是）',
  `exception_msg` varchar(500) DEFAULT NULL COMMENT '异常信息',
  `user_id` varchar(50) DEFAULT 'anonymous' COMMENT '调用链路的用户ID',
  `user_name` varchar(50) DEFAULT 'anonymous' COMMENT '调用链路的用户姓名',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_trace_id` (`trace_id`) USING BTREE,
  KEY `idx_trace_start_time` (`trace_start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='完整的链路信息';

-- ----------------------------
-- Records of trace_walking_compete
-- ----------------------------

-- ----------------------------
-- Table structure for trace_walking_method
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trace_walking_method` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` varchar(50) NOT NULL COMMENT '链路的唯一标识',
  `span_id` varchar(20) NOT NULL COMMENT '子链路的标识（进入子链路的时间戳）',
  `span_start_time` datetime(3) DEFAULT NULL COMMENT '子链路开始的时间',
  `span_end_time` datetime(3) DEFAULT NULL COMMENT '子链路的结束时间',
  `span_time_consume` int(11) DEFAULT '0' COMMENT '子链路的耗时（单位：毫秒）',
  `method_start_time` datetime(3) DEFAULT NULL COMMENT '方法调用的开始时间',
  `method_end_time` datetime(3) DEFAULT NULL COMMENT '方法调用的结束时间',
  `method_time_consume` int(11) DEFAULT '0' COMMENT '方法调用的耗时（单位：毫秒）',
  `consumer_server_name` varchar(100) DEFAULT NULL COMMENT '方法所在消费端服务的名称',
  `provider_server_name` varchar(100) DEFAULT NULL COMMENT '方法所在调用端服务的名称',
  `request_url` varchar(200) DEFAULT NULL COMMENT '链路方法的请求入口的URL',
  `class_name` varchar(200) DEFAULT NULL COMMENT '方法所在的类',
  `method_name` varchar(150) DEFAULT NULL COMMENT '方法名',
  `exception_flag` tinyint(1) DEFAULT '0' COMMENT '是否异常方法（0：否 1：是）',
  `exception_msg` varchar(500) DEFAULT NULL COMMENT '异常信息',
  `invoke_order` int(11) DEFAULT '0' COMMENT '方法执行的顺序号',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`) USING BTREE,
  KEY `idx_method_name` (`method_name`) USING BTREE,
  KEY `idx_method_start_time` (`method_start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='链路方法信息';

-- ----------------------------
-- Records of trace_walking_method
-- ----------------------------

-- ----------------------------
-- Table structure for trace_walking_server
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trace_walking_server` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `caller_server_name` varchar(100) NOT NULL COMMENT '调用方服务的名称',
  `callee_server_name` varchar(100) NOT NULL COMMENT '被调用方服务的名称',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_caller_callee` (`caller_server_name`,`callee_server_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='服务之间的调用信息';

-- ----------------------------
-- Records of trace_walking_server
-- ----------------------------

-- ----------------------------
-- Table structure for trace_walking_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trace_walking_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(50) NOT NULL COMMENT '密码',
  `enable` tinyint(1) DEFAULT '1' COMMENT '是否有效（0：注销 1：有效）',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='链路登录用户信息';