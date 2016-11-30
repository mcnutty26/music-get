//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

class YoutubeDownload implements Runnable {

    private String URL;
    private ProcessQueue process_queue;
    private String ip;
    private String directory;

    YoutubeDownload(String URL, ProcessQueue process_queue, String ip, String directory) {
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
                        , "-o", directory + guid, "--restrict-filenames", URL);
                Process dl;
                dl = downloader.start();
                dl.waitFor();

                //try and rename the file just in case youtubedl added an extention
                ProcessBuilder renamer = new ProcessBuilder("/bin/bash", "-c", "mv " + directory + guid + ".* " + directory + guid);
                Process rn;
                rn = renamer.start();
                rn.waitFor();

                String real_name = sb.toString();
                System.out.println("Downloaded file " + real_name + " for " + ip);
                if (!process_queue.new_item(new QueueItem(guid, real_name, ip)))
                {
                    System.out.println("Rejected file " + real_name + " from " + ip + " - too many items queued");
                    Files.delete(Paths.get(directory + guid));
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
