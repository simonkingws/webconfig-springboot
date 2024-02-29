package com.simonkingws.webconfig.core.config;

import com.simonkingws.webconfig.common.constant.SymbolConstant;
import com.simonkingws.webconfig.common.core.WebconfigProperies;
import com.simonkingws.webconfig.core.Interceptor.RequestContextInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

/**
 * 自动装配
 *
 * @author: ws
 * @date: 2024/1/30 19:04
 */
@Configuration
@ConditionalOnClass(WebconfigProperies.class)
@EnableConfigurationProperties(WebconfigProperies.class)
@ComponentScan("com.simonkingws.webconfig")
public class WebconfigAutoConfigration implements WebMvcConfigurer {

    @Autowired
    private WebconfigProperies webconfigProperies;
    @Autowired
    private RequestContextInterceptor requestContextIntercepter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String excludePathPatterns = webconfigProperies.getExcludePathPatterns();
        String[] epps = {};
        if (StringUtils.isNotBlank(excludePathPatterns)) {
            epps = excludePathPatterns.split(SymbolConstant.COMMA);
        }

        registry.addInterceptor(requestContextIntercepter)
                .addPathPatterns(webconfigProperies.getInterceptorPathPatterns().split(SymbolConstant.COMMA))
                .excludePathPatterns(epps)
                .order(Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * Validator 校验快速失败配置
     */
    @Override
    public Validator getValidator() {
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                .configure()
                .failFast(webconfigProperies.getValidFailFast())
                .buildValidatorFactory();

        return new SpringValidatorAdapter(validatorFactory.getValidator());
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
