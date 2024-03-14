package com.simonkingws.webconfig.trace.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.trace.admin.mapper.TraceWalkingMethodMapper;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingMethod;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingMethodService;
import com.simonkingws.webconfig.trace.admin.vo.ServerInvokeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 链路方法信息 服务实现类
 * </p>
 *
 * @author ws
 * @since 2024-03-05
 */
@Slf4j
@Service
public class TraceWalkingMethodServiceImpl implements TraceWalkingMethodService {

    @Autowired
    private TraceWalkingMethodMapper traceWalkingMethodMapper;

    @Override
    public void processTraceWalkingMethod(List<TraceItem> traceItems) {
        List<TraceWalkingMethod> methodList = new ArrayList<>();
        traceItems.forEach(item -> {
            TraceWalkingMethod method = new TraceWalkingMethod();
            method.setTraceId(item.getTraceId());
            method.setSpanId(item.getSpanId().toString());
            method.setSpanStartTime(new Date(item.getSpanId()));
            method.setSpanEndTime(new Date(item.getSpanEndMs()));
            method.setSpanTimeConsume((int)(item.getSpanEndMs() - item.getSpanId()));
            method.setConsumerServerName(item.getConsumerApplicatName());
            method.setProviderServerName(item.getProviderApplicatName());
            method.setMethodName(item.getMethodName());
            method.setMethodStartTime(new Date(item.getInvokeStartTime()));
            method.setMethodEndTime(new Date(item.getInvokeEndTime()));
            method.setMethodTimeConsume((int)(item.getInvokeEndTime() - item.getInvokeStartTime()));
            method.setInvokeOrder(item.getOrder());
            method.setCreatedTime(new Date());

            methodList.add(method);
        });

        // 数据入库
        traceWalkingMethodMapper.insertBatch(methodList);
        log.info("......[TraceWalkingMethod]数据采集成功......");
    }

    @Override
    public List<ServerInvokeVO> statServerInvokeCount() {
        return traceWalkingMethodMapper.statServerInvokeCount();
    }

    @Override
    public List<TraceWalkingMethod> getMethodTraceList(String traceId) {
        LambdaQueryWrapper<TraceWalkingMethod> queryWrapper = Wrappers.lambdaQuery(TraceWalkingMethod.class)
                .eq(TraceWalkingMethod::getTraceId, traceId)
                .orderByDesc(TraceWalkingMethod::getInvokeOrder);
        return traceWalkingMethodMapper.selectList(queryWrapper);
    }
}
