package acme.http;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadExecutorWrapper extends AbstractExecutorService {
    protected final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void shutdown() {
        executor.shutdown();
    }
    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }
    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }
    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }
    @Override
    public void execute(Runnable task) {
        this.executor.execute(() -> {
            final var currentThread = Thread.currentThread();
            currentThread.setName("VirtualThread-" + currentThread.threadId());
            beforeExecute(currentThread, task);
            try {
                task.run();
                afterExecute(task, null);
            } catch (Throwable t) {
                afterExecute(task, t);
                throw t;
            }
        });
    }
    protected void beforeExecute(final Thread t, final Runnable r) {}
    protected void afterExecute(final Runnable r, final Throwable t) {}
}
