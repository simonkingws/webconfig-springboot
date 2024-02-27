package com.simonkingws.webconfig.core.handler;

import com.alibaba.fastjson2.JSON;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.core.JsonResult;
import com.simonkingws.webconfig.common.exception.WebConfigException;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.common.util.SpringContextHolder;
import com.simonkingws.webconfig.core.resolver.GlobalExceptionResponseResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 全局异常处理器
 *
 * @author: ws
 * @date: 2024/1/29 11:04
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static String LOG_TEMPLATE = "请求路径[url]={}，[%s] 异常信息：";
    public static String REQUEST_PARAMS_EX_TEMPLATE = "参数[%s]：%s";
    public static String REQUEST_PARAMS_MISS_TEMPLATE = "请求参数缺失，缺失的参数：%s";

    @Autowired(required = false)
    private GlobalExceptionResponseResolver globalExceptionResponseResolver;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验参数绑定异常
     *
     * @author ws
     * @date 2024/1/29 11:13
     * @param e
     */
    @ExceptionHandler(BindException.class)
    public Object bindExceptionHandler(HttpServletRequest request, BindException e) {
        log.error(String.format(LOG_TEMPLATE, "bindExceptionHandler"), request.getRequestURL(), e);
        return doGenerateResult(e, getErrorMsgDefault(e.getBindingResult().getFieldErrors(), e.getMessage()));
    }

    /**
     * 方法参数校验异常处理
     *
     * @author ws
     * @date 2024/1/29 13:25
     * @param e
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object methodArgumentNotValidExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException e) {
        log.error(String.format(LOG_TEMPLATE, "methodArgumentNotValidExceptionHandler"), request.getRequestURL(), e);
        return doGenerateResult(e, getErrorMsgDefault(e.getBindingResult().getFieldErrors(), e.getMessage()));
    }

    /**
     * 请求参数缺失异常
     *
     * @author ws
     * @date 2024/1/29 13:33
     * @param e
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object missingServletRequestParameterExceptionHandler(HttpServletRequest request, MissingServletRequestParameterException e) {
        log.error(String.format(LOG_TEMPLATE, "missingServletRequestParameterExceptionHandler"), request.getRequestURL(), e);
        return doGenerateResult(e, String.format(REQUEST_PARAMS_MISS_TEMPLATE, e.getParameterName()));
    }

    /**
     * 重复的请求拦截异常
     *
     * @author ws
     * @date 2024/1/30 16:51
     * @param request
     * @param e
     */
    @ExceptionHandler(WebConfigException.class)
    public Object requestLimitingInterceptorExceptionHandler(HttpServletRequest request, WebConfigException e) {
        log.error(String.format(LOG_TEMPLATE, "requestLimitingInterceptorExceptionHandler"), request.getRequestURL(), e);
        return doGenerateResult(e, e.getMessage());
    }

    /**
     * 兜底的异常处理
     *
     * @author ws
     * @date 2024/1/29 13:35
     * @param e
     */
    @ExceptionHandler(Exception.class)
    public Object exceptionHandler(HttpServletRequest request, Exception e) {
        log.error(String.format(LOG_TEMPLATE, "exceptionHandler"), request.getRequestURL(), e);
        return doGenerateResult(e, e.getMessage());
    }

    /**
     * 处理公用的参数校验异常的信息
     *
     * @author ws
     * @date 2024/1/31 10:45
     */
    private String getErrorMsgDefault(List<FieldError> fieldErrors, String detaultMsg) {
        String errorMsg = null;
        if (!CollectionUtils.isEmpty(fieldErrors)) {
            List<String> msgs = new ArrayList<>(fieldErrors.size());
            fieldErrors.forEach(error -> msgs.add(showErrorMsg(error)));
            errorMsg = String.join(";", msgs);
        }
        return Optional.ofNullable(errorMsg).orElse(detaultMsg);
    }

    private String showErrorMsg(FieldError fieldError) {
        return String.format(REQUEST_PARAMS_EX_TEMPLATE, fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * 处理通用结果
     *
     * @author ws
     * @date 2024/1/31 10:43
     */
    private Object doGenerateResult(Exception e, String errorMsg) {
        disposeExceptionTrace(errorMsg);
        if (globalExceptionResponseResolver != null) {
            globalExceptionResponseResolver.pushExceptionNotice(e, errorMsg);
            return globalExceptionResponseResolver.resolveExceptionResponse(e, errorMsg);
        }
        return JsonResult.ofFail(errorMsg);
    }

    private void disposeExceptionTrace(String errorMsg) {
        try {
            // 保存链路信息到redis, 链路太长容易引起性能问题
            RequestContextLocal local = RequestHolder.get();
            if (local != null) {
                List<String> range = stringRedisTemplate.opsForList().range(local.getTraceId(), 0, -1);
                if (!CollectionUtils.isEmpty(range)) {
                    long count = range.stream().map(item -> JSON.parseObject(item, TraceItem.class))
                            .filter(item -> item.getMethodName().startsWith(TraceConstant.EXCEPTION_TRACE_PREFIX))
                            .count();
                    if (count == 0) {
                        String applicationName = SpringContextHolder.getApplicationName();

                        TraceItem traceItem = TraceItem.copy2TraceItem(local);
                        traceItem.setConsumerApplicatName(applicationName);
                        traceItem.setProviderApplicatName(applicationName);
                        traceItem.setMethodName(TraceConstant.EXCEPTION_TRACE_PREFIX + errorMsg);

                        stringRedisTemplate.opsForList().rightPush(local.getTraceId(), JSON.toJSONString(traceItem));
                    }
                }
            }
        }catch (Exception e){
            log.warn("disposeExceptionTrace 异常：", e);
        }
    }
}
