package batchDebug;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class MoveTask {
    private Timer cron;
    private File source;
    private File destination;

    public MoveTask(File source, File destination) {
        this.source = source;
        this.destination = destination;
        cron = new Timer();
    }

    public void scheduleFileMove(String string, int integer) {
        TimerTask moveTimerTask = new MoveTimerTask(source, destination, string);
        cron.schedule(moveTimerTask, 1, TimeUnit.SECONDS.toMillis(integer));
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
            //System.out.println(String.format("Move task for mask '%s' is scheduled to run every %d hour(s)", string, PERIOD));
        }

        @Override
        public void run() {
            try {
                File[] directoryListing = source.listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        if (Files.isRegularFile(child.toPath(), LinkOption.NOFOLLOW_LINKS) && child.getName().toLowerCase().contains(string.toLowerCase())) {
                            System.out.println("directoryListing child.getName = " + child.getName());
                            Files.move(child.toPath(), destination.toPath().resolve(child.getName()));
                            System.out.println(" directoryListing child moved to  = " + destination.toPath().resolve(child.getName()));
                        }
                    }
                }
            } catch (
                    IOException ignore) {
            }
        }
    }
}
