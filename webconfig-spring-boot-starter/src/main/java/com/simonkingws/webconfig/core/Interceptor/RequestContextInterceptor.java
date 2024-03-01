package com.simonkingws.webconfig.core.Interceptor;

import com.simonkingws.webconfig.common.constant.MDCKey;
import com.simonkingws.webconfig.common.constant.RequestHeaderConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.core.WebconfigProperies;
import com.simonkingws.webconfig.common.process.RequestContextLocalPostProcess;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.common.util.SpringContextHolder;
import com.simonkingws.webconfig.common.util.TraceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

/**
 * 全局参数拦截器
 *
 * @author: ws
 * @date: 2024/1/31 16:12
 */
@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private RequestContextLocalPostProcess requestContextLocalPostProcess;
    @Autowired
    private WebconfigProperies webconfigProperies;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!webconfigProperies.getRequestContextInterceptor()) {
            return false;
        }

        RequestContextLocal requestContextLocal = RequestContextLocal.buildContext(request);
        if (isRpcInvoke(request)) {
            // 采集链路信息
            collectTraceData(request, requestContextLocal);
        }else{
            requestContextLocal.setOpenTraceCollect(webconfigProperies.getOpenTraceCollect());
            if (requestContextLocalPostProcess != null) {
                requestContextLocalPostProcess.afterRequestContextLocal(requestContextLocal);
            }
        }

        RequestHolder.add(requestContextLocal);
        MDC.put(MDCKey.TRACEID, requestContextLocal.getTraceId());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (isRpcInvoke(request)) {
            traceDataSave();
        }else {
            RequestHolder.remove(requestContextLocalPostProcess);
        }
    }

    private boolean isRpcInvoke(HttpServletRequest request) {
        String feignMark = request.getHeader(RequestHeaderConstant.FIEGN_MARK_KEY);
        return StringUtils.isNotBlank(feignMark);
    }

    private void traceDataSave() {
        // 保存链路信息到redis, 链路太长容易引起性能问题
        RequestContextLocal local = RequestHolder.get();
        if (local != null && BooleanUtils.isTrue(local.getOpenTraceCollect())) {
            try {
                // 补充首次进入链路信息的结束时间
                if (TraceContextHolder.isNotEmpty()) {
                    TraceContextHolder.getTraceItems().get(0).setSpanEndMs(Instant.now().toEpochMilli());
                    stringRedisTemplate.opsForList().rightPushAll(local.getTraceId(), TraceContextHolder.toStringList());

                    TraceContextHolder.remove();
                }
            }catch (Exception e){
                log.warn("redis未配置或reids服务没有启动：{}", e.getMessage());
            }
        }

        RequestHolder.remove();
    }

    private void collectTraceData(HttpServletRequest request, RequestContextLocal requestContextLocal) {
        if (BooleanUtils.isTrue(requestContextLocal.getOpenTraceCollect())) {
            log.info(">>>>>Feign调用-链路信息采集启用中......");
            String feignMethodName = request.getHeader(RequestHeaderConstant.FIEGN_METHOD_NAME);
            String consumerApplicationName = request.getHeader(RequestHeaderConstant.FIEGN_CONSUMER_APPLICATION_NAME);
            String applicationName = SpringContextHolder.getApplicationName();

            TraceItem traceItem = TraceItem.copy2TraceItem(requestContextLocal);
            traceItem.setConsumerApplicatName(consumerApplicationName);
            traceItem.setProviderApplicatName(applicationName);
            traceItem.setMethodName(feignMethodName);

            // 链路信息保存在线程中
            TraceContextHolder.addTraceItem(traceItem);
            // 上下文设置远程调用的方法
            requestContextLocal.setRpcMethodName(feignMethodName);
        }else{
            log.info(">>>>>>Feign调用-链路信息采集已被禁用中......");
        }
    }
}
