package com.simonkingws.webconfig.common.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Webconfig 项目的配置信息
 *
 * @author: ws
 * @date: 2024/1/30 18:58
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "springboot.webconfig")
public class WebconfigProperies {

    /**
     * 重复请求的缓存模式
     */
    private String requestLimitCacheMode = "redis";

    /**
     * 是否开启检验参数完成后返回所有错误信息，默认一个参数异常就返回
     */
    private Boolean validFailFast = true;

    /**
     * 是否开启全局请求参数拦截
     */
    private Boolean requestContextInterceptor = true;

    /**
     * 全局请求参数拦截路径
     */
    private String interceptorPathPatterns = "/**";

    /**
     * 全局请求参数忽略路径
     */
    private String excludePathPatterns;

    /**
     * 是否开启dubbo全局请求参数拦截
     */
    private Boolean dubboInterceptor = true;

    /**
     * 是否开启Feign全局请求头参数拦截
     */
    private Boolean feignInterceptor = true;

    /**
     * 是否开启链路信息采集
     */
    private Boolean openTraceCollect = false;

}
