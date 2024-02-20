package com.simonkingws.service2.impl;

import com.simonkingws.api.service2.Book2Service;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * TODO
 *
 * @author: ws
 * @date: 2024/2/2 18:52
 */
@Service("book2Service")
@DubboService
public class Book2ServiceImpl implements Book2Service {
    @Override
    public BigDecimal getBook2Price() {
        return BigDecimal.valueOf(100.88);
    }
}
