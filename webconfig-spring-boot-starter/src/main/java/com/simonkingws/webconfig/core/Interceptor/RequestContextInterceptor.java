package com.simonkingws.webconfig.core.Interceptor;

import com.alibaba.fastjson2.JSON;
import com.simonkingws.webconfig.common.constant.MDCKey;
import com.simonkingws.webconfig.common.constant.RequestHeaderConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.core.WebconfigProperies;
import com.simonkingws.webconfig.common.process.RequestContextLocalPostProcess;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.common.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        if (requestContextLocalPostProcess != null) {
            requestContextLocalPostProcess.afterRequestContextLocal(requestContextLocal);
        }
        
        RequestHolder.add(requestContextLocal);
        MDC.put(MDCKey.TRACEID, requestContextLocal.getTraceId());

        // 保存链路信息到redis, 链路太长容易引起性能问题
        String feignMark = request.getHeader(RequestHeaderConstant.FIEGN_MARK_KEY);
        if (StringUtils.isNotBlank(feignMark)) {
            try {
                RequestContextLocal local = RequestHolder.get();
                if (local != null) {
                    String feignMethodName = request.getHeader(RequestHeaderConstant.FIEGN_METHOD_NAME);
                    String consumerApplicationName = request.getHeader(RequestHeaderConstant.FIEGN_CONSUMER_APPLICATION_NAME);
                    String applicationName = SpringContextHolder.getApplicationName();

                    TraceItem traceItem = TraceItem.copy2TraceItem(local);
                    traceItem.setConsumerApplicatName(consumerApplicationName);
                    traceItem.setProviderApplicatName(applicationName);
                    traceItem.setMethodName(feignMethodName);

                    stringRedisTemplate.opsForList().rightPush(local.getTraceId(), JSON.toJSONString(traceItem));
                }
            }catch (Exception e){
                log.warn("redis未配置或reids服务没有启动：", e);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String feignMark = request.getHeader(RequestHeaderConstant.FIEGN_MARK_KEY);
        if (StringUtils.isBlank(feignMark)) {
            RequestHolder.remove(requestContextLocalPostProcess);
        }else{
            RequestHolder.remove();
        }
        MDC.remove(MDCKey.TRACEID);
    }
}
