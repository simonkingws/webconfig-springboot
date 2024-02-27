package com.simonkingws.webconfig.core.aspect;

import com.alibaba.fastjson2.JSON;
import com.simonkingws.webconfig.common.constant.SymbolConstant;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.common.util.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * <code>@InnerTrace</code>的切面
 *
 * @author: ws
 * @date: 2024/2/27 9:11
 */
@Slf4j
@Aspect
@Component
public class TraceAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Pointcut("@annotation(com.simonkingws.webconfig.core.annotation.InnerTrace)")
    public void pointcut(){

    }

    @Around("pointcut()")
    public Object Around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName  = null;
        Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();
        if (ArrayUtils.isNotEmpty(interfaces)) {
            for (Class<?> anInterface : interfaces) {
                Method[] methods = anInterface.getMethods();
                if (ArrayUtils.isNotEmpty(methods)) {
                    long count = Arrays.stream(methods).filter(m -> method.getName().equals(m.getName())).count();
                    if (count > 0) {
                        methodName = anInterface.getName() + SymbolConstant.DOT + method.getName() + SymbolConstant.BRACKET;
                        break;
                    }
                }
            }
        }
        if (StringUtils.isBlank(methodName)) {
            // 本地方法
            methodName = method.getDeclaringClass().getName() + SymbolConstant.DOT + method.getName() + SymbolConstant.BRACKET;
        }
        log.info("@InnerTrace[{}]>>>>>>开始处理>>>>>>>>>>", methodName);

        RequestContextLocal local = RequestHolder.get();
        if (Optional.ofNullable(local).map(RequestContextLocal::getRpcMethodName).orElse("").equals(methodName)) {
            return joinPoint.proceed();
        }

        Object proceed;
        TraceItem traceItem = null;
        try {
            if (local != null) {
                Integer traceSum = local.getTraceSum();
                traceSum++;

                local.setEndPos(methodName);
                local.setTraceSum(traceSum);

                String applicationName = SpringContextHolder.getApplicationName();

                traceItem = TraceItem.copy2TraceItem(local);
                traceItem.setConsumerApplicatName(applicationName);
                traceItem.setProviderApplicatName(applicationName);
                traceItem.setMethodName(methodName);
            }

            // 执行方法
            proceed = joinPoint.proceed();

            try {
                if (local != null) {
                    traceItem.setSpanEndMs(Instant.now().toEpochMilli());
                    stringRedisTemplate.opsForList().leftPush(local.getTraceId(), JSON.toJSONString(traceItem));
                }
            }catch (Exception e){
                log.warn("处理@InnerTrace注解异常：", e);
            }
        }catch (Throwable th){
            // 保存链路信息到redis, 链路太长容易引起性能问题
            try {
                if (local != null && traceItem != null) {
                    traceItem.setSpanEndMs(Instant.now().toEpochMilli());

                    String traceId = local.getTraceId();
                    stringRedisTemplate.opsForList().leftPush(traceId, JSON.toJSONString(traceItem));

                    // 增肌异常信息的处理
                    traceItem.setMethodName(TraceConstant.EXCEPTION_TRACE_PREFIX + th.getMessage());
                    stringRedisTemplate.opsForList().rightPush(traceId, JSON.toJSONString(traceItem));
                }
            }catch (Exception e){
                log.warn("处理@InnerTrace注解异常：", e);
            }

            // 重新抛出异常不印象业务系统的捕获
            throw th;
        }

        return proceed;
    }
}
