package com.simonkingws.application.demos.web;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * TODO
 *
 * @author: ws
 * @date: 2024/2/1 14:02
 */
@Data
public class Book implements Serializable {

    private static final long serialVersionUID = -4746928436252252305L;

    /**
     *  ID
     */
    private Integer bookId;

    /**
     *  名称
     */
    @Length(min = 5, max = 20, message = "书名的长度必须在5~20之间")
    private String bookName;

    /**
     *  定价
     */
    @Min(value = 15, message = "书的价格不能低于15元")
    @Max(value = 100, message = "书的价格不能超过100元")
    private BigDecimal bookPrice;

    /**
     *  发布日期
     */
    @NotNull(message = "publishDate 不能为空")
    private Date publishDate;

    /**
     *  编写完成日期
     */
    @NotNull(message = "finishDateTime 不能为空")
    private LocalDateTime finishDateTime;

    /**
     * script
     */
    private String script;

}
