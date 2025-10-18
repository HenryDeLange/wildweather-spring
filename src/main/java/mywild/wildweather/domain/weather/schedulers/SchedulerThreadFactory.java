package mywild.wildweather.domain.weather.schedulers;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerThreadFactory implements ThreadFactory {

    private final AtomicInteger counter = new AtomicInteger(1);

    private final String name;

    public SchedulerThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, String.format(name + "%02d", counter.getAndIncrement()));
    }

}
