package com.ivotasevski.idempotency.config;

import com.ivotasevski.idempotency.filter.IdempotentHandlingFilter;
import jakarta.servlet.DispatcherType;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@AllArgsConstructor
@EnableJpaAuditing
@EnableScheduling
@Configuration
public class IdempotencyConfig {

    @Bean
    public TransactionTemplate getTransactionTemplate(PlatformTransactionManager platformTransactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate;
    }

    @Bean
    public FilterRegistrationBean<IdempotentHandlingFilter> idempotentFilter(IdempotentHandlingFilter filter) {
        FilterRegistrationBean<IdempotentHandlingFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return reg;
    }

    @Bean(destroyMethod = "")
    public ThreadPoolTaskExecutor compensationExecutor() {
        // TODO: Make properties configurable
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("CompensationExecutor-");
        executor.initialize();
        return executor;
    }

}
