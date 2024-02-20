package com.simonkingws.webconfig.dubbo3.filter;

import com.alibaba.fastjson.JSON;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.util.RequestHolder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 全局Filter
 *
 * @author: ws
 * @date: 2023/5/17 9:51
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class DubboRpcFilter implements Filter {

    // 共享的dubbo全局key
    public static final String RPC_CONTEXT_KEY = "dubbo3:rpc:request:context";

    // 这里只能通过Set方法注入
    @Setter
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获取本地信息
        RpcServiceContext rpcContext = RpcContext.getServiceContext();
        String invokeMethodName = String.format(TraceConstant.INVOKE_METHOND_NAME, rpcContext.getUrl().getServiceKey(), rpcContext.getMethodName());
        if (rpcContext.isConsumerSide()) {
            // 消费端： 同步本地信息（写入dubbo的上下文中）
            log.info(">>>>>>>消费端：进入DubboRpcFilter，拦截[{}]方法，将本地上下文保存在dubbo中", invokeMethodName);
            RpcContextAttachment clientAttachment = RpcContext.getClientAttachment();
            clientAttachment.setAttachment(RPC_CONTEXT_KEY,  RequestHolder.get());
        }
        if (rpcContext.isProviderSide()) {
            // 提供端：增加链路信息
            RpcContextAttachment serverAttachment = RpcContext.getServerAttachment();
            RequestContextLocal local = (RequestContextLocal)serverAttachment.getObjectAttachment(RPC_CONTEXT_KEY);
            if (local != null) {
                String applicationName = rpcContext.getRemoteApplicationName();
                log.info(">>>>>>>提供端：进入DubboRpcFilter，拦截[{}]方法被调用，将dubbo上下文的数据保存到本地上下文", invokeMethodName);

                Integer traceSum = Optional.ofNullable(local.getTraceSum()).orElse(0);
                traceSum++;

                String traceWalking = StringUtils.defaultString(local.getTraceWalking(), "");
                String currentTraceWalking = String.format(TraceConstant.INVOKE_TRACE, traceSum, applicationName, invokeMethodName);

                local.setTraceSum(traceSum);
                local.setEndPos(invokeMethodName);
                local.setTraceWalking(traceWalking + currentTraceWalking);

                // 将处理好的数据放进本地上下文
                RequestHolder.add(local);
                log.info("当前请求的[traceId:{}]的链路追踪轨迹：{}", local.getTraceId(), local.getTraceWalking());
                if (stringRedisTemplate != null) {
                    try {
                        stringRedisTemplate.opsForValue().set(local.getTraceId(), JSON.toJSONString(local), 1, TimeUnit.HOURS);
                    }catch (Exception e){
                        log.info("redis未配置或reids服务没有启动：", e);
                    }
                }
            }
        }

        return invoker.invoke(invocation);
    }
}