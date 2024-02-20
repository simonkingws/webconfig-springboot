package com.simonkingws.webconfig.feign.interceptor;

import com.alibaba.fastjson2.JSON;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.core.WebconfigProperies;
import com.simonkingws.webconfig.common.util.RequestHolder;
import feign.MethodMetadata;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Fiegn 请求头请求拦截器
 *
 * @author: ws
 * @date: 2024/1/31 17:55
 */
@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    @Autowired
    private WebconfigProperies webconfigProperies;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (BooleanUtils.isTrue(webconfigProperies.getFeignInterceptor())) {
            RequestContextLocal requestContextLocal = RequestHolder.get();
            if (requestContextLocal != null) {
                // 获取调用的方法
                MethodMetadata methodMetadata = requestTemplate.methodMetadata();
                String invokeMethodName = methodMetadata.targetType().getName() + "." + methodMetadata.method().getName() + "()";
                String applicationName = requestTemplate.feignTarget().name();

                // 增加链路信息
                Integer traceSum = Optional.ofNullable(requestContextLocal.getTraceSum()).orElse(0);
                traceSum++;

                String traceWalking = StringUtils.defaultString(requestContextLocal.getTraceWalking(), "");
                String currentTraceWalking = String.format(TraceConstant.INVOKE_TRACE, traceSum, applicationName, invokeMethodName);

                requestContextLocal.setTraceSum(traceSum);
                requestContextLocal.setEndPos(invokeMethodName);
                requestContextLocal.setTraceWalking(traceWalking + currentTraceWalking);

                requestTemplate.headers(requestContextLocal.context2HeaderMap());

                log.info("当前Feign请求的[traceId:{}]的链路追踪轨迹：{}", requestContextLocal.getTraceId(), requestContextLocal.getTraceWalking());
                if (stringRedisTemplate != null) {
                    try {
                        stringRedisTemplate.opsForValue().set(requestContextLocal.getTraceId(), JSON.toJSONString(requestContextLocal), 1, TimeUnit.HOURS);
                    }catch (Exception e){
                        log.info("redis未配置或reids服务没有启动：", e);
                    }
                }
            }
        }
    }
}
