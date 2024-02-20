package com.simonkingws.webconfig.common.constant;

/**
 * 请求头常量
 *
 * @author: ws
 * @date: 2024/1/31 15:24
 */
public interface RequestHeaderConstant {

    /**
     * 每次请求的唯一标识
     */
    String TRACE_ID = "trace-id";

    /**
     *  链路的条数
     */
    String TRACE_SUM = "trace-sum";

    /**
     *  链路起始
     */
    String START_POS = "start-pos";

    /**
     *  链路起始
     */
    String END_POS = "end-pos";

    /**
     *  链路轨迹
     */
    String TRACE_WALKING = "trace-walking";

    /**
     *  链路轨迹起始时间
     */
    String TRACE_START_MS = "trace-start-ms";

    /**
     *  用户ID
     */
    String USER_ID = "user-id";

    /**
     *  用户名称
     */
    String USER_NAME = "user-name";

    /**
     *  用户类型
     */
    String USER_TYPE = "user-type";

    /**
     *  登录的令牌
     */
    String TOKEN = "token";

    String[] HEADERS = {TRACE_ID, TRACE_SUM, START_POS, END_POS, TRACE_WALKING, USER_ID, USER_NAME, USER_TYPE, TOKEN};
}
