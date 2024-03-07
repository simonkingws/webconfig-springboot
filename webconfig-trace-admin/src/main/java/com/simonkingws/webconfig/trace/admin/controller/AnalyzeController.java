package com.simonkingws.webconfig.trace.admin.controller;

import com.simonkingws.webconfig.common.core.JsonResult;
import com.simonkingws.webconfig.trace.admin.dto.TraceWalkingDTO;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingCompete;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingServer;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingCompeteService;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingMethodService;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingServerService;
import com.simonkingws.webconfig.trace.admin.vo.ServerInvokeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分析采集的链路数据
 *
 * @author: ws
 * @date: 2024/3/6 15:20
 */
@Slf4j
@RestController
@RequestMapping("/analyze")
public class AnalyzeController {

    @Autowired
    private TraceWalkingServerService traceWalkingServerService;
    @Autowired
    private TraceWalkingMethodService traceWalkingMethodService;
    @Autowired
    private TraceWalkingCompeteService traceWalkingCompeteService;

    /**
     * 服务拓扑图的数据
     *
     * @author ws
     * @date 2024/3/7 10:46
     */
    @GetMapping("/topologyMap")
    public JsonResult<?> topologyMap(){
        List<TraceWalkingServer> serverList = traceWalkingServerService.getAllServerList();
        return JsonResult.ofSuccess(serverList);
    }

    /**
     * 服务列表被调用次数柱状图
     *
     * @author ws
     * @date 2024/3/7 10:52
     */
    @GetMapping("/serverHistogram")
    public JsonResult<?> serverHistogram(){
        List<ServerInvokeVO> invokeList = traceWalkingMethodService.statServerInvokeCount();
        return JsonResult.ofSuccess(invokeList);
    }

    /**
     * 根据条件获取Topx的数据
     *
     * @author ws
     * @date 2024/3/7 11:17
     */
    @GetMapping("/competeTopX")
    public JsonResult<?> competeTopX(TraceWalkingDTO traceWalkingDto){
        List<TraceWalkingCompete> competeList = traceWalkingCompeteService.getCompeteByCondition(traceWalkingDto);
        return JsonResult.ofSuccess(competeList);
    }

}
