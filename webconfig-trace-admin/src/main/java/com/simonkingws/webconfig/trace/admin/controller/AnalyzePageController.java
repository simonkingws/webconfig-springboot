package com.simonkingws.webconfig.trace.admin.controller;

import com.simonkingws.webconfig.trace.admin.dto.TraceWalkingDTO;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingCompete;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingCompeteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 分析采集的链路数据
 *
 * @author: ws
 * @date: 2024/3/6 15:20
 */
@Slf4j
@Controller
@RequestMapping("/analyze")
public class AnalyzePageController {

    @Autowired
    private TraceWalkingCompeteService traceWalkingCompeteService;


    /**
     * 根据条件获取Topx的数据
     *
     * @author ws
     * @date 2024/3/14 13:17
     */
    @GetMapping("/loadCompeteTopX")
    public String loadCompeteTopX(TraceWalkingDTO traceWalkingDto, Model model) {
        List<TraceWalkingCompete> competeList = traceWalkingCompeteService.getCompeteByCondition(traceWalkingDto);
        model.addAttribute("walkingCompeteList", competeList);
        return "index::walkingCompeteList";
    }
}
