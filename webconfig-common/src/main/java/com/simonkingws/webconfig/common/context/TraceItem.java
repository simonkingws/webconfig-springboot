package com.simonkingws.webconfig.common.context;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 子链路信息
 *
 * @author: ws
 * @date: 2024/2/26 15:54
 */
@Data
@Builder
public class TraceItem implements Serializable {

    private static final long serialVersionUID = 8345886464536102086L;

    /**
     *  每次请求链路的唯一标识
     */
    private String traceId;

    /**
     *  子链路的标识，也是子链路的开始时间：时间戳
     */
    private Long spanId;

    /**
     *  子链路结束时间
     */
    private Long spanEndMs;

    /**
     *  调用方法消费端的服务名称
     */
    private String consumerApplicatName;

    /**
     *  调用方法提供端的服务名称
     */
    private String providerApplicatName;

    /**
     *  调用的方法
     */
    private String methodName;

    /**
     *  调用的顺序
     */
    private Integer order;


    public static TraceItem copy2TraceItem(RequestContextLocal local){
        return TraceItem.builder()
                .traceId(local.getTraceId())
                .spanId(local.getSpanId())
                .order(local.getTraceSum())
                .build();
    }
}
