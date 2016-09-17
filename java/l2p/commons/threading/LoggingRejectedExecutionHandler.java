package l2p.commons.threading;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author NB4L1
 */
public final class LoggingRejectedExecutionHandler implements RejectedExecutionHandler {

    private static final Logger _log = LoggerFactory.getLogger(LoggingRejectedExecutionHandler.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            return;
        }

        _log.error(r + " from " + executor, new RejectedExecutionException());
    }
}
