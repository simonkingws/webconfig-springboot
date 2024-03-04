package com.simonkingws.webconfig.core.aspect;

import com.simonkingws.webconfig.common.constant.RedisConstant;
import com.simonkingws.webconfig.common.constant.RequestHeaderConstant;
import com.simonkingws.webconfig.common.core.WebconfigProperies;
import com.simonkingws.webconfig.common.exception.WebConfigException;
import com.simonkingws.webconfig.core.annotation.SubmitLimiting;
import com.simonkingws.webconfig.core.contant.RunMode;
import com.simonkingws.webconfig.core.resolver.CacheResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @SubmitLimiting 注解的切面
 *
 * @author: ws
 * @date: 2024/3/4 14:36
 */
@Slf4j
@Aspect
@Component
public class SubmitLimitingAspect {

    @Autowired
    private WebconfigProperies webconfigProperies;
    @Autowired
    private Map<String, CacheResolver> cacheResolverMap;

    @Pointcut("@annotation(com.simonkingws.webconfig.core.annotation.SubmitLimiting)")
    public void pointcut(){

    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert requestAttributes != null;

        CacheResolver cacheResolver = cacheResolverMap.get(webconfigProperies.getRequestLimitCacheMode());

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        log.info("@SubmitLimiting注解拦截方法[{}()]>>>>>>开始处理>>>>>>>>>>", method.getName());
        String cacheKey = String.format(RedisConstant.CACHE_KEY, method.getName());
        SubmitLimiting[] annotations = method.getAnnotationsByType(SubmitLimiting.class);
        // 多个@RequestLimiting， 取其中一个
        SubmitLimiting submitLimiting = annotations[0];
        if (submitLimiting.mode().equals(RunMode.INIT)) {
            HttpServletResponse response = requestAttributes.getResponse();
            assert response != null;
            response.setHeader(RequestHeaderConstant.ONCE_ACCESS_TOKEN, UUID.randomUUID().toString());
        }else if (submitLimiting.mode().equals(RunMode.VALID)) {
            // 验证是否已经访问过
            HttpServletRequest request = requestAttributes.getRequest();
            String header = request.getHeader(RequestHeaderConstant.ONCE_ACCESS_TOKEN);
            if (StringUtils.isBlank(header)) {
                throw new WebConfigException("请求头中没有包含校验重复提交的参数！");
            }
            Boolean ifAbsent = cacheResolver.setIfAbsent(cacheKey, header, 30, TimeUnit.MINUTES);
            if (!ifAbsent) {
                throw new WebConfigException("该请求已提交，请勿重复操作！");
            }
        }

        return joinPoint.proceed();
    }
}
