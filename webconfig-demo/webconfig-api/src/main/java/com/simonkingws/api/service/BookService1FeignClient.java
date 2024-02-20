package com.simonkingws.api.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * TODO
 *
 * @author: ws
 * @date: 2024/2/5 16:03
 */
@FeignClient(name = "service1", contextId = "bookService1")
public interface BookService1FeignClient {

    @GetMapping("/feign/bookName")
    String bookName(@RequestParam("bookId") Integer bookId);

    @GetMapping("/feign/checkDate")
    String checkDate(@RequestParam("date") Date date);

    @GetMapping("/feign/checkLocalDateTime")
    String checkLocalDateTime(@RequestParam("localDateTime") LocalDateTime localDateTime);
}
