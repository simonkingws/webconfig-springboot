package com.simonkingws.service2.impl;

import com.simonkingws.api.service2.BookService2FeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;


@Slf4j
@RestController
public class BookService2Controller implements BookService2FeignClient {

    @Override
    public BigDecimal getBook2Price() {
        return new BigDecimal(1000);
    }
}
