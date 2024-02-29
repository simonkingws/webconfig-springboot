package com.simonkingws.webconfig.core.aspect;

import com.simonkingws.webconfig.common.constant.SymbolConstant;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.common.util.SpringContextHolder;
import com.simonkingws.webconfig.common.util.TraceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

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
        RequestContextLocal local = RequestHolder.get();
        if (local == null || BooleanUtils.isNotTrue(local.getOpenTraceCollect()) || TraceContextHolder.isEmpty()) {
            return joinPoint.proceed();
        }

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

        // 判断该方法是不是Rpc调用的：如果是就忽略，rpc会自己采集
        if (StringUtils.equals(methodName, local.getRpcMethodName())) {
            return joinPoint.proceed();
        }

        // 采集来链路信息
        String applicationName = SpringContextHolder.getApplicationName();
        Integer traceSum = local.getTraceSum();
        traceSum++;
        try {
            Long size = stringRedisTemplate.opsForList().size(local.getTraceId());
            if (size != null) {
                traceSum += size.intValue();
            }
        }catch (Exception e) {
            log.warn("方法[{}]中的redis 配置未配置或配置异常：{}", methodName, e.getMessage());
        }

        local.setEndPos(methodName);
        local.setTraceSum(traceSum);

        TraceItem traceItem = TraceItem.copy2TraceItem(local);
        traceItem.setConsumerApplicatName(applicationName);
        traceItem.setProviderApplicatName(applicationName);
        traceItem.setMethodName(methodName);

        List<TraceItem> traceItemList = TraceContextHolder.getTraceItems();
        Object proceed;
        try {
            // 执行方法
            proceed = joinPoint.proceed();

            traceItem.setSpanEndMs(Instant.now().toEpochMilli());
            traceItemList.add(traceItem);
        }catch (Throwable th){
            // 有异常需要记录异常的链路信息，并将异常抛出
            try {
                traceItem.setSpanEndMs(Instant.now().toEpochMilli());
                traceItemList.add(traceItem);

                // 增肌异常信息的处理
                TraceItem exceptionTraceItem = TraceItem.builder().build();
                BeanUtils.copyProperties(traceItem, exceptionTraceItem);
                exceptionTraceItem.setMethodName(TraceConstant.EXCEPTION_TRACE_PREFIX + th.getMessage());
                traceItemList.add(exceptionTraceItem);
            }catch (Exception e){
                log.warn("处理@InnerTrace注解异常：", e);
            }

            // 重新抛出异常不印象业务系统的捕获
            throw th;
        }

        return proceed;
    }
}
