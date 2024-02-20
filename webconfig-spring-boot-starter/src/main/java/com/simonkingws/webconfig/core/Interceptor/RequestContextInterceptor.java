package com.simonkingws.webconfig.core.Interceptor;

import com.simonkingws.webconfig.common.constant.MDCKey;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.core.WebconfigProperies;
import com.simonkingws.webconfig.common.process.RequestContextLocalPostProcess;
import com.simonkingws.webconfig.common.util.RequestHolder;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private RequestContextLocalPostProcess requestContextLocalPostProcess;
    @Autowired
    private WebconfigProperies webconfigProperies;


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
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        RequestHolder.remove(requestContextLocalPostProcess);
        MDC.remove(MDCKey.TRACEID);
    }
}
