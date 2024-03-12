package com.simonkingws.webconfig.trace.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.simonkingws.webconfig.common.constant.TraceConstant;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.trace.admin.dto.TraceWalkingDTO;
import com.simonkingws.webconfig.trace.admin.mapper.TraceWalkingCompeteMapper;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingCompete;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingCompeteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 完整的链路信息 服务实现类
 * </p>
 *
 * @author ws
 * @since 2024-03-05
 */
@Slf4j
@Service
public class TraceWalkingCompeteServiceImpl implements TraceWalkingCompeteService {

    @Autowired
    private TraceWalkingCompeteMapper traceWalkingCompeteMapper;

    @Override
    public void processTraceWalkingCompelete(List<TraceItem> traceItems) {
        TraceWalkingCompete compete = new TraceWalkingCompete();

        // 解析链路的入口数据
        TraceItem traceItem = traceItems.get(0);
        compete.setTraceId(traceItem.getTraceId());
        compete.setTraceStartTime(new Date(traceItem.getSpanId()));
        compete.setApplicationName(traceItem.getConsumerApplicatName());
        compete.setTraceStartPos(traceItem.getMethodName());

        // 解析链路的出口数据
        TraceItem lastTraceItem = traceItems.get(traceItems.size() -1);
        compete.setTraceEndTime(new Date(lastTraceItem.getSpanEndMs()));
        compete.setTraceEndPos(lastTraceItem.getMethodName());
        compete.setExceptionFlag(lastTraceItem.getMethodName().startsWith(TraceConstant.EXCEPTION_TRACE_PREFIX));

        // 解析汇总数据
        compete.setTraceSum((int)traceItems.stream().map(TraceItem::getSpanId).distinct().count());
        compete.setInvokeMethodSum(traceItems.size());
        compete.setTraceTimeConsume((int)(lastTraceItem.getSpanEndMs() - traceItem.getSpanId()));
        compete.setCreatedTime(new Date());

        // 数据入库
        traceWalkingCompeteMapper.insert(compete);
        log.info("......[TraceWalkingCompete]数据采集成功......");

    }

    @Override
    public List<TraceWalkingCompete> getCompeteByCondition(TraceWalkingDTO traceWalkingDto) {
        LambdaQueryWrapper<TraceWalkingCompete> queryWrapper = Wrappers.lambdaQuery(TraceWalkingCompete.class)
                .eq(StringUtils.isNoneBlank(traceWalkingDto.getTraceId()), TraceWalkingCompete::getTraceId, traceWalkingDto.getTraceId())
                .eq(traceWalkingDto.getExceptionFlag() != null, TraceWalkingCompete::getExceptionFlag, traceWalkingDto.getExceptionFlag())
                .ge(traceWalkingDto.getInvokeTimeStart() != null, TraceWalkingCompete::getTraceStartTime, traceWalkingDto.getInvokeTimeStart())
                .le(traceWalkingDto.getInvokeTimeEnd() != null, TraceWalkingCompete::getTraceEndTime, traceWalkingDto.getInvokeTimeEnd())
                .orderByDesc(TraceWalkingCompete::getTraceStartTime)
                .last("LIMIT " + Optional.ofNullable(traceWalkingDto.getTopSum()).orElse(10));
        return traceWalkingCompeteMapper.selectList(queryWrapper);
    }
}
