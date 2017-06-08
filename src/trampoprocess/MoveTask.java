package trampoprocess;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveTask {
    static final Logger LOG = LoggerFactory.getLogger(MoveTask.class);

    private Timer cron;
    private File source;
    private File destination;

    public MoveTask(File source, File destination) {
        this.source = source;
        this.destination = destination;
        cron = new Timer();
    }

    public void scheduleFileMove(String string, int integer) {
        LOG.debug("scheduleFileMove start");
        TimerTask moveTimerTask = new MoveTimerTask(source, destination, string);
        LOG.debug("TimerTask moveTimerTask = new MoveTimerTask done");
        try {
        cron.schedule(moveTimerTask, 1, TimeUnit.SECONDS.toMillis(integer)); // this line is the issue with the installer
             
        }
        catch(Exception e){
        LOG.error("test exception",e);
        }
        LOG.debug("cron.schedule(moveTimerTask done");
    }

    public void cancelPurgeTimer() {
        cron.cancel();
        cron.purge();
    }

    class MoveTimerTask extends TimerTask {

        File source;
        File destination;
        String string;

        public MoveTimerTask(File source, File destination, String string) {
            this.source = source;
            this.destination = destination;
            this.string = string;
            //LOG.debug(String.format("Move task for mask '%s' is scheduled to run every %d hour(s)", string, PERIOD));
        }

        @Override
        public void run() {
            try {
                File[] directoryListing = source.listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS) && child.getName().toLowerCase().contains(string.toLowerCase())) {
                            LOG.debug("directoryListing child.getName = " + child.getName());
                            Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                            LOG.debug(" directoryListing child moved to  = " + destination.toPath().resolve(child.getName()));
                        }
                    }
                }
            } catch (IOException ignore) {
            }
        }
    }
}
