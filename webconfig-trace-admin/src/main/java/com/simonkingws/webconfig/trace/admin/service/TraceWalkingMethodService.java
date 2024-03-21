package com.simonkingws.webconfig.trace.admin.service;

import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.trace.admin.dto.MethodInvokeDTO;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingMethod;
import com.simonkingws.webconfig.trace.admin.vo.MethodStatVO;
import com.simonkingws.webconfig.trace.admin.vo.ServerInvokeVO;

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

    List<ServerInvokeVO> statServerInvokeCount();

    List<TraceWalkingMethod> getMethodTraceList(String traceId);

    List<MethodStatVO> getMethodInvokeStat(MethodInvokeDTO methodInvokeDto);
}
