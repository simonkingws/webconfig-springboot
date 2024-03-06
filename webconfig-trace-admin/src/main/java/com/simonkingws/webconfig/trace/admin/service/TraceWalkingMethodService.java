package com.simonkingws.webconfig.trace.admin.service;

import com.simonkingws.webconfig.common.context.TraceItem;

import java.util.List;

/**
 * <p>
 * 链路方法信息 服务类
 * </p>
 *
 * @author ws
 * @since 2024-03-05
 */
public interface TraceWalkingMethodService {

    void processTraceWalkingMethod(List<TraceItem> traceItems);
}
