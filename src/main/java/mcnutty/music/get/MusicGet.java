//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MusicGet {

    public static void main(String[] args) throws Exception {

        //print out music-get
        System.out.println("                     _                      _   ");
        System.out.println(" _ __ ___  _   _ ___(_) ___       __ _  ___| |_ ");
        System.out.println("| '_ ` _ \\| | | / __| |/ __|____ / _` |/ _ \\ __|");
        System.out.println("| | | | | | |_| \\__ \\ | (_|_____| (_| |  __/ |_ ");
        System.out.println("|_| |_| |_|\\__,_|___/_|\\___|     \\__, |\\___|\\__|");
        System.out.println("                                 |___/          ");

        //these will always be initialised later (but the compiler doesn't know that)
        int timeout = 0;
        int buckets = 0;
        String directory = "";
        Properties prop = new Properties();
        InputStream input;
        try {
            input = new FileInputStream("config.properties");
            prop.load(input);
            if (prop.getProperty("buckets") != null){
                buckets = Integer.parseInt(prop.getProperty("buckets"));
            } else {
                System.out.println("Error reading config property 'buckets' - using default value of 4\n");
                buckets = 4;
            }
            if (prop.getProperty("timeout") != null){
                timeout = Integer.parseInt(prop.getProperty("timeout"));
            } else {
                System.out.println("Error reading config property 'timeout' - using default value of 547\n");
                timeout = 547;
            }
            if (prop.getProperty("directory") != null) {
                directory = prop.getProperty("directory");
            } else {
                System.out.println("Error reading config property 'directory' - using default value of /tmp/musicserver/\n");
                directory = "/tmp/musicserver/";
            }
            if (prop.getProperty("password") == null) {
                System.out.println("Error reading config property 'password' - no default value, exiting\n");
                System.exit(1);
            }
        } catch (IOException e) {
            System.out.println("Error reading config file");
            System.exit(1);
        }

        //clean out the directory where music files will go
        try {
            Files.walkFileTree(Paths.get(directory), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            Files.delete(Paths.get(directory));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Files.createDirectory(Paths.get(directory));

        //create a queue object
        ProcessQueue process_queue = new ProcessQueue(buckets);

        //start the web server
        StartServer start_server = new StartServer(process_queue, directory);
        new Thread(start_server).start();

        //wit for the web server to spool up
        Thread.sleep(1000);

        //read items from the queue and play them
        while (true) {
            QueueItem next_item = process_queue.next_item();
            if (!next_item.equals(new QueueItem())) {
                System.out.println("Playing " + next_item.real_name);
                process_queue.set_played(next_item);
                Process p = Runtime.getRuntime().exec("timeout " + timeout + "s mplayer -fs -quiet -af volnorm=2:0.25 "
                        + directory + next_item.disk_name);

                try {
                    p.waitFor(timeout, TimeUnit.SECONDS);
                    Files.delete(Paths.get(directory + next_item.disk_name));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                process_queue.bucket_played.clear();
            }
            Thread.sleep(1000);
        }
    }

}
