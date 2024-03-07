package com.simonkingws.webconfig.trace.admin.controller;

import com.simonkingws.webconfig.common.core.JsonResult;
import com.simonkingws.webconfig.trace.admin.model.TraceWalkingUser;
import com.simonkingws.webconfig.trace.admin.service.TraceWalkingUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 用户信息控制层
 *
 * @author: ws
 * @date: 2024/3/7 14:57
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private TraceWalkingUserService traceWalkingUserService;

    /**
     * 注销用户
     *
     * @author ws
     * @date 2024/3/7 14:53
     */
    @PostMapping("/disabledUser")
    public JsonResult<?> disabledUser(@NotBlank(message = "用户名不能为空！") String username) {
        TraceWalkingUser user = traceWalkingUserService.getUserByUsername(username);
        Assert.notNull(user, "该用户信息已不存在！");

        traceWalkingUserService.disabledUser(user);
        return JsonResult.ofSuccess();
    }
}
