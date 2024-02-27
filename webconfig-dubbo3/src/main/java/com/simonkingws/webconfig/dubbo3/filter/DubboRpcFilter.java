package com.simonkingws.webconfig.dubbo3.filter;

import com.alibaba.fastjson.JSON;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.common.util.SpringContextHolder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 全局Filter
 *
 * @author: ws
 * @date: 2023/5/17 9:51
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class DubboRpcFilter implements Filter, BaseFilter.Listener {

    // 共享的dubbo全局key
    public static final String RPC_CONTEXT_KEY = "dubbo3:rpc:request:context";
    public static final String RPC_CONTEXT_TRACE_ITEM = "dubbo3:rpc:trace:item";

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
                log.info(">>>>>>>提供端：进入DubboRpcFilter，拦截[{}]方法被调用，将dubbo上下文的数据保存到本地上下文", invokeMethodName);

                Integer traceSum = Optional.ofNullable(local.getTraceSum()).orElse(0);
                traceSum++;

                local.setTraceSum(traceSum);
                local.setEndPos(invokeMethodName);
                local.setRpcMethodName(invokeMethodName);

                // 将处理好的数据放进本地上下文
                RequestHolder.add(local);

                // 封装链路信息
                TraceItem traceItem = TraceItem.copy2TraceItem(local);
                traceItem.setMethodName(invokeMethodName);
                traceItem.setConsumerApplicatName(serverAttachment.getRemoteApplicationName());
                traceItem.setProviderApplicatName(SpringContextHolder.getApplicationName());
                serverAttachment.setAttachment(RPC_CONTEXT_TRACE_ITEM, traceItem);
            }
        }

        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        // 获取本地信息
        RpcServiceContext rpcContext = RpcContext.getServiceContext();
        RpcContextAttachment serverAttachment = RpcContext.getServerAttachment();
        TraceItem traceItem = (TraceItem)serverAttachment.getObjectAttachment(RPC_CONTEXT_TRACE_ITEM);
        if (rpcContext.isProviderSide() && traceItem != null) {
            // 保存链路信息到redis, 链路太长容易引起性能问题
            traceItem.setSpanEndMs(Instant.now().toEpochMilli());
            try {
                if (appResponse.hasException()) {
                    String traceId = traceItem.getTraceId();
                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForList().rightPush(traceId, JSON.toJSONString(traceItem));

                        List<String> range = stringRedisTemplate.opsForList().range(traceId, 0, -1);
                        long count = 0;
                        if (!CollectionUtils.isEmpty(range)) {
                            count = range.stream().map(item -> com.alibaba.fastjson2.JSON.parseObject(item, TraceItem.class))
                                    .filter(item -> item.getMethodName().startsWith(TraceConstant.EXCEPTION_TRACE_PREFIX))
                                    .count();
                        }
                        if (appResponse.hasException() && count == 0) {
                            traceItem.setMethodName(TraceConstant.EXCEPTION_TRACE_PREFIX + appResponse.getException().getMessage());

                            String applicationName = SpringContextHolder.getApplicationName();
                            traceItem.setConsumerApplicatName(serverAttachment.getRemoteApplicationName());
                            traceItem.setProviderApplicatName(applicationName);

                            stringRedisTemplate.opsForList().leftPush(traceId, JSON.toJSONString(traceItem));
                        }
                    }
                }
            }catch (Exception e){
                log.warn("redis未配置或reids服务没有启动：{}", e.getMessage());
            }

            // 调用结束之后需要删除本地线程的数据，防止OOM
            RequestHolder.remove();
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

    }
}