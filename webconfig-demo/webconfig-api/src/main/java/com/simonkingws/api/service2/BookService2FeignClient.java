package com.simonkingws.api.service2;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

/**
 * TODO
 *
 * @author: ws
 * @date: 2024/2/5 16:03
 */
@FeignClient(name = "service2", contextId = "bookService2")
public interface BookService2FeignClient {

    @GetMapping("/getBook2Price")
    BigDecimal getBook2Price();
}
