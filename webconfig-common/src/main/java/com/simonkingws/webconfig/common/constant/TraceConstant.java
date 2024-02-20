package com.simonkingws.webconfig.common.constant;

/**
 * 调用链路常量
 *
 * @author: ws
 * @date: 2024/2/20 14:43
 */
public interface TraceConstant {

    /** 调用的方法名称 */
    String INVOKE_METHOND_NAME = "%s.%s()";
    /** 链路追踪的名称 */
    String INVOKE_TRACE = "->([%d][%s][%s])";
}
