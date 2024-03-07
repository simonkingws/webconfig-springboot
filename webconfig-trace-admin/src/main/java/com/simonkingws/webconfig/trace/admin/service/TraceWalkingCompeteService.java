package com.simonkingws.webconfig.trace.admin.service;

import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.trace.admin.dto.TraceWalkingDTO;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingCompete;

import java.util.List;

/**
 * <p>
 * 完整的链路信息 服务类
 * </p>
 *
 * @author ws
 * @since 2024-03-05
 */
public interface TraceWalkingCompeteService {

    void processTraceWalkingCompelete(List<TraceItem> traceItems);

    List<TraceWalkingCompete> getCompeteByCondition(TraceWalkingDTO traceWalkingDto);
}
