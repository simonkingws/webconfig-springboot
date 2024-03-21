package com.simonkingws.webconfig.trace.admin.controller;

import com.simonkingws.webconfig.common.constant.MDCKey;
import com.simonkingws.webconfig.common.context.RequestContextLocal;
import com.simonkingws.webconfig.common.context.TraceItem;
import com.simonkingws.webconfig.common.util.RequestHolder;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingCompeteService;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingMethodService;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingServerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * 链路信息采集控制层
 *
 * @author: ws
 * @date: 2024/3/5 17:13
 */
@Slf4j
@RestController
@RequestMapping("/trace")
public class TraceController {

    @Autowired
    private Executor customThreadPoolExecutor;
    @Autowired
    private TraceWalkingCompeteService traceWalkingCompeteService;
    @Autowired
    private TraceWalkingMethodService traceWalkingMethodService;
    @Autowired
    private TraceWalkingServerService traceWalkingServerService;

    /**
     * 采集加链路信息
     *
     * @author ws
     * @date 2024/3/5 17:16
     */
    @RequestMapping("/collect")
    public void collect(@RequestBody List<TraceItem> traceItems){
        if (!CollectionUtils.isEmpty(traceItems)) {
            customThreadPoolExecutor.execute(() -> {
                MDC.put(MDCKey.TRACEID, Optional.ofNullable(RequestHolder.get()).map(RequestContextLocal::getTraceId).orElse(""));
                try {
                    // 1、处理完整的链路信息
                    traceWalkingCompeteService.processTraceWalkingCompelete(traceItems);

                    // 2、处理链路方法
                    traceWalkingMethodService.processTraceWalkingMethod(traceItems);

                    // 3、处理链路服务
                    traceWalkingServerService.processTraceWalkingServer(traceItems);
                    log.info("......完整链路数据采集成功......");
                }finally {
                    MDC.clear();
                }
            });
        }
    }
}
