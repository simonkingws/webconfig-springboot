package com.simonkingws.webconfig.common.util;


import com.alibaba.fastjson2.JSON;
import com.simonkingws.webconfig.common.constant.MDCKey;
import com.simonkingws.webconfig.common.constant.SymbolConstant;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.process.RequestContextLocalPostProcess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    public static void remove() {
        THREAD_LOCAL.remove();
        MDC.remove(MDCKey.TRACEID);
    }

    public static void remove(RequestContextLocalPostProcess requestContextLocalPostProcess) {
        log.info("当前请求的本地副本被清除.");
        final RequestContextLocal local = THREAD_LOCAL.get();
        if (local != null) {
            local.setTraceEndMs(Instant.now().toEpochMilli());
        }
        THREAD_LOCAL.remove();
        
        // 异步处理链路数据
        asyncHandleTrace(local, requestContextLocalPostProcess);
    }

    /**
     * 异步处理李娜烤炉信息
     *
     * @author ws
     * @date 2024/2/26 18:59
     */
    private static void asyncHandleTrace(RequestContextLocal local, RequestContextLocalPostProcess requestContextLocalPostProcess) {
        CompletableFuture.runAsync(() -> {
            if (local != null) {
                String traceId = local.getTraceId();
                try {
                    if (StringUtils.isNotBlank(traceId)) {
                        // 重置MDC
                        MDC.put(MDCKey.TRACEID, traceId);

                        String applicationName = SpringContextHolder.getApplicationName();
                        // 构建初始化的方法
                        TraceItem traceBegin = TraceItem.copy2TraceItem(local);
                        traceBegin.setOrder(0);
                        traceBegin.setMethodName(local.getStartPos());
                        traceBegin.setConsumerApplicatName(applicationName);
                        traceBegin.setProviderApplicatName(applicationName);
                        traceBegin.setSpanEndMs(local.getTraceEndMs());

                        List<TraceItem> traceItemList = new ArrayList<>();
                        traceItemList.add(traceBegin);
                        // 处理缓存数据
                        List<String> traces = stringRedisTemplate.opsForList().range(traceId, 0, -1);
                        if (!CollectionUtils.isEmpty(traces)) {
                            List<TraceItem> collect = traces.stream().map(traceItem -> JSON.parseObject(traceItem, TraceItem.class)).collect(Collectors.toList());

                            List<TraceItem> nomalTraceItemList = collect.stream().filter(item -> !item.getMethodName().startsWith(TraceConstant.EXCEPTION_TRACE_PREFIX)).collect(Collectors.toList());
                            List<TraceItem> exTraceItemList = collect.stream().filter(item -> item.getMethodName().startsWith(TraceConstant.EXCEPTION_TRACE_PREFIX)).collect(Collectors.toList());
                            if (!CollectionUtils.isEmpty(exTraceItemList)) {
                                TraceItem traceItem = exTraceItemList.get(0);
                                traceItem.setOrder(nomalTraceItemList.size() + 1);
                                nomalTraceItemList.add(traceItem);
                            }

                            traceItemList.addAll(nomalTraceItemList);
                            local.setTraceSum(nomalTraceItemList.size());
                        }

                        // 清除缓存的的链路数据
                        stringRedisTemplate.delete(traceId);

                        // 输出链路信息
                        String traceWalking = getCompeleteTraceWalking(traceItemList);
                        log.info("当前请求的完整的链路信息：{}", traceWalking);

                        local.setTraceWalking(traceWalking);
                    }

                    // 调用销毁的方法
                    if (requestContextLocalPostProcess != null) {
                        requestContextLocalPostProcess.destroy(local);
                    }
                }catch (Exception e){
                    log.info("redis未配置或reids服务异常：", e);
                }finally {
                    // 清除MDC的key
                    if (StringUtils.isNotBlank(traceId)) {
                        MDC.remove(traceId);
                    }
                }
            }
        });
    }

    private static String getCompeleteTraceWalking(List<TraceItem> traceItemList) {
        StringBuilder sb = new StringBuilder();
        traceItemList.forEach(item -> {
            String trace = String.format(TraceConstant.INVOKE_TRACE, item.getOrder(), item.getConsumerApplicatName(), item.getProviderApplicatName(), item.getMethodName());
            sb.append(trace).append(SymbolConstant.ARROW);
        });

        return sb.substring(0, sb.length() - SymbolConstant.ARROW.length());
    }
}
