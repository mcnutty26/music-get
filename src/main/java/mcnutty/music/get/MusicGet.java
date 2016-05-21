//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class MusicGet {

    private static final String TEMP_DIR = "musicserver";

    private static final int SEVER_TIMEOUT = 547;

    public static void main(String[] args) throws Exception {
        final Path filesPath = Paths.get(System.getProperty("java.io.tmpdir"), TEMP_DIR);

        // Print out music-get
        System.out.println("                     _                      _   ");
        System.out.println(" _ __ ___  _   _ ___(_) ___       __ _  ___| |_ ");
        System.out.println("| '_ ` _ \\| | | / __| |/ __|____ / _` |/ _ \\ __|");
        System.out.println("| | | | | | |_| \\__ \\ | (_|_____| (_| |  __/ |_ ");
        System.out.println("|_| |_| |_|\\__,_|___/_|\\___|     \\__, |\\___|\\__|");
        System.out.println("                                 |___/          ");

        // Cleanup temp directories
        cleanupMusicFiles(filesPath);

        // Create the process queue
        ProcessQueue processQueue = new ProcessQueue();

        // Start the web server and wait for it to spool up
        StartServer server = new StartServer(processQueue, TEMP_DIR);
        new Thread(server).start();
        Thread.sleep(1000);

        // Read items from the queue and play them
        while (true) {
            QueueItem nextItem = processQueue.nextItem();
            if (!nextItem.equals(new QueueItem())) {
                Path itemPath = Paths.get(filesPath.toString(), nextItem.getDiskName());

                // Play next item in the queue
                System.out.println("Playing " + nextItem.getRealName());
                processQueue.setPlayed(nextItem);
                Process p = Runtime.getRuntime()
                        .exec("timeout " + SEVER_TIMEOUT + "s mplayer -fs -quiet -af volnorm=2:0.25 " + itemPath.toString());

                try {
                    p.waitFor(SEVER_TIMEOUT, TimeUnit.SECONDS);
                    Files.delete(itemPath);
                } catch (Exception ignored) {
                }
            } else {
                processQueue.getBucketPlayed().clear();
            }
            Thread.sleep(1000);
        }
    }

    private static void cleanupMusicFiles(Path filesPath) {
        try {
            // Clean out the directory where music files will go
            Files.walkFileTree(filesPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            Files.delete(filesPath);

            // Re-create files path for future use
            Files.createDirectory(filesPath);
        } catch (Exception ignored) {
        }
    }
}