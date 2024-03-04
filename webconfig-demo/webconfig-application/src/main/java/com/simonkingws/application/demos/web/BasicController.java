package com.simonkingws.application.demos.web;

import com.simonkingws.api.service.BookService;
import com.simonkingws.api.service2.Book2Service;
import com.simonkingws.webconfig.common.constant.DateFormatConstant;
import com.simonkingws.webconfig.core.annotation.RequestLimiting;
import com.simonkingws.webconfig.core.annotation.SubmitLimiting;
import com.simonkingws.webconfig.core.contant.Policy;
import com.simonkingws.webconfig.core.contant.RunMode;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.format.DateTimeFormatter;

/**
 * 测试控制类
 *
 * @author ws
 * @date 2024/2/1 13:59
 */
@RestController
@RequestMapping("foo")
public class BasicController {

    @DubboReference
    private BookService bookService;
    @DubboReference
    private Book2Service book2Service;

    /**
     * 测试Form表单提交
     */
    @RequestMapping("/testFormData")
    public String testFormData(@Valid Book book){
        System.out.println(book);
        System.out.println(DateTimeFormatter.ofPattern(DateFormatConstant.STANDARD_DATE_TIME).format(book.getFinishDateTime()));
        return "success";
    }

    /**
     * 测试JSON
     */
    @RequestMapping("/testJSON")
    public String testJSON(@RequestBody @Validated Book book){
        System.out.println(book);
        System.out.println(DateTimeFormatter.ofPattern(DateFormatConstant.STANDARD_DATE_TIME).format(book.getFinishDateTime()));
        return "success";
    }

    /**
     * 测试@RequestLimiting
     */
    @RequestMapping("/testLimiting")
    @RequestLimiting(returnPolicy = Policy.IGNORE)
    public String testLimiting(Book book) throws InterruptedException {
        Thread.sleep(2000);
        System.out.println(book);
        return "success";
    }

    /**
     * 测试dubbo
     *
     * @author ws
     * @date 2024/2/2 14:12
     */
    @RequestMapping("/testDubbo3")
    public String getBookName(Integer bookId)  {
        return bookService.bookName(bookId);
    }

    @RequestMapping("/testDubbo4")
    public String testDubbo4()  {
        return book2Service.getBook2Price().toString();
    }

    @RequestMapping("/testSubmitInit")
    @SubmitLimiting(mode = RunMode.INIT)
    public String testSubmitInit()  {
        return "init success";
    }

    @RequestMapping("/testSubmitValid")
    @SubmitLimiting
    public String testSubmitValid()  {
        return "submitValid success";
    }
}
