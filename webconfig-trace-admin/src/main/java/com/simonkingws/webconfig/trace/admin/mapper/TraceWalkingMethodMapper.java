package com.simonkingws.webconfig.trace.admin.mapper;

import com.simonkingws.webconfig.trace.admin.mapper.generate.GenerateMapper;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingMethod;
import com.simonkingws.webconfig.trace.admin.vo.ServerInvokeVO;

import java.util.List;

/**
 * <p>
 * 链路方法信息 Mapper 接口
 * </p>
 *
 * @author ws
 * @since 2024-03-05
 */
public interface TraceWalkingMethodMapper extends GenerateMapper<TraceWalkingMethod> {

    List<ServerInvokeVO> statServerInvokeCount();
}
