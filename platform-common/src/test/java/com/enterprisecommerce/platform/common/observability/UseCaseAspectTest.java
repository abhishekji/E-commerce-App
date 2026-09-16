package com.enterprisecommerce.platform.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UseCaseAspectTest {
    @Test
    void recordsTimingForUseCaseAnnotation() throws Throwable {
        MeterRegistry registry = new SimpleMeterRegistry();
        UseCaseAspect aspect = new UseCaseAspect(registry);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");

        UseCase annotation = SampleUseCase.class.getDeclaredMethod("run").getAnnotation(UseCase.class);
        Object result = aspect.measure(joinPoint, annotation);

        assertEquals("ok", result);
        Timer timer = registry.find("commerce.usecase.duration").tag("usecase", "sample.search").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    private static class SampleUseCase {
        @UseCase("sample.search")
        public String run() {
            return "ok";
        }
    }
}
