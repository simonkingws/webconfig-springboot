package com.simonkingws.webconfig.common.util;


import com.alibaba.fastjson2.JSON;
import com.simonkingws.webconfig.common.constant.MDCKey;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.process.RequestContextLocalPostProcess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * 全局请求参数管理器
 *
 * @author ws
 * @date 2024/1/31 16:07
 */
@Slf4j
public class RequestHolder {

    private static final StringRedisTemplate stringRedisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

    private RequestHolder() {}

    private static final ThreadLocal<RequestContextLocal> THREAD_LOCAL = new InheritableThreadLocal<>();

    public static void add(RequestContextLocal requestContextLocal) {
        THREAD_LOCAL.set(requestContextLocal);
    }

    public static RequestContextLocal get() {
        return THREAD_LOCAL.get();
    }

    public static void remove(RequestContextLocalPostProcess requestContextLocalPostProcess) {
        final RequestContextLocal local = THREAD_LOCAL.get();
        if (local != null) {
            local.setTraceEndMs(Instant.now().toEpochMilli());
        }
        THREAD_LOCAL.remove();

        // 异步处理链路数据
        CompletableFuture.runAsync(() -> {
            if (local != null) {
                String traceId = local.getTraceId();
                try {
                    RequestContextLocal finalLocal = local;
                    if (StringUtils.isNotBlank(traceId)) {
                        // 重置MDC
                        MDC.put(MDCKey.TRACEID, traceId);

                        String requestContextLocalJson = stringRedisTemplate.opsForValue().get(traceId);
                        if (StringUtils.isNotBlank(requestContextLocalJson)) {
                            finalLocal = JSON.parseObject(requestContextLocalJson, RequestContextLocal.class);
                        }

                        // 清除缓存的的链路数据
                        stringRedisTemplate.delete(traceId);
                    }

                    // 调用销毁的方法
                    if (requestContextLocalPostProcess != null) {
                        requestContextLocalPostProcess.destroy(finalLocal);
                    }
                }catch (Exception e){
                    log.info("redis未配置或reids服务没有启动或反序列化异常：", e);
                }finally {
                    // 清除MDC的key
                    if (StringUtils.isNotBlank(traceId)) {
                        MDC.remove(traceId);
                    }
                }
            }
        });
    }
}
