package com.enterprisecommerce.platform.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UseCaseAspect {
    private final MeterRegistry meterRegistry;

    public UseCaseAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(useCase)")
    public Object measure(ProceedingJoinPoint joinPoint, UseCase useCase) throws Throwable {
        long started = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            meterRegistry.timer("commerce.usecase.duration", "usecase", useCase.value())
                    .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }
}
