//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

public class YoutubeDownload implements Runnable {

    String URL;
    ProcessQueue process_queue;
    String ip;
    String directory;

    public YoutubeDownload(String URL, ProcessQueue process_queue, String ip, String directory) {
        this.URL = URL;
        this.process_queue = process_queue;
        this.ip = ip;
        this.directory = directory;
    }

    @Override
    public void run() {
        //prevent downloading the wrong video if the link was part of a playlist
        if (URL.contains("youtube.com") && URL.contains("&list")) {
            URL = URL.substring(0, URL.indexOf("&list"));
        }

        //set the item as downloading
        System.out.println("Starting download of video " + URL + " queued by " + ip);
        QueueItem temp = new QueueItem("null", URL, ip);
        process_queue.bucket_youtube.add(temp);
        String guid = UUID.randomUUID().toString();

        //get the name of the item
        ProcessBuilder pb = new ProcessBuilder(
                Paths.get("").toAbsolutePath().toString() + "/youtube-dl", "--get-filename"
                , "-o%(title)s", "--restrict-filenames", "--no-playlist", URL);
        Process p;
        try {
            p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            //download the actual file and add it to the queue
            if (!sb.toString().equals("")) {
                ProcessBuilder downloader = new ProcessBuilder(Paths.get("").toAbsolutePath().toString() + "/youtube-dl"
                        , "-o", System.getProperty("java.io.tmpdir") + directory + guid, "--restrict-filenames", "-f mp4", URL);
                Process dl;
                dl = downloader.start();
                dl.waitFor();

                String extension = "";
                String real_name = sb.toString();
                System.out.println("Downloaded file " + real_name + " for " + ip);
                if (!process_queue.new_item(new QueueItem(guid + extension, real_name, ip)))
                {
                    System.out.println("Rejected file " + real_name + " from " + ip + " - too many items queued");
                    Files.delete(Paths.get(System.getProperty("java.io.tmpdir") + directory + guid + extension));
                }
            } else {
                System.out.println("Could not download video " + URL + " queued by " + ip);
            }

            //set the download as completed
            process_queue.bucket_youtube.remove(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}