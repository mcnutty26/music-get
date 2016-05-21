//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.UUID;

class YoutubeDownload implements Runnable {

    private String url;
    private ProcessQueue processQueue;
    private String ip;
    private String directory;

    YoutubeDownload(String url, ProcessQueue processQueue, String ip, String directory) {
        this.url = url;
        this.processQueue = processQueue;
        this.ip = ip;
        this.directory = directory;
    }

    @Override
    public void run() {
        //prevent downloading the wrong video if the link was part of a playlist
        if (url.contains("youtube.com") && url.contains("&list")) {
            url = url.substring(0, url.indexOf("&list"));
        }

        //set the item as downloading
        System.out.println("Starting download of video " + url + " queued by " + ip);
        QueueItem temp = new QueueItem("null", url, ip);
        processQueue.getBucketYoutube().add(temp);
        String guid = UUID.randomUUID().toString();

        //get the name of the item
        ProcessBuilder pb = new ProcessBuilder(
                Paths.get("").toAbsolutePath().toString() + "/youtube-dl", "--get-filename"
                , "-o%(title)s", "--restrict-filenames", "--no-playlist", url);
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
                        , "-o", System.getProperty("java.io.tmpdir") + directory + guid, "--restrict-filenames", "-f mp4", url);
                Process dl;
                dl = downloader.start();
                dl.waitFor();

                String extension = "";
                String realName = sb.toString();
                processQueue.newItem(new QueueItem(guid + extension, realName, ip));
                System.out.println("Downloaded file " + realName + " for " + ip);
            } else {
                System.out.println("Could not download video " + url + " queued by " + ip);
            }

            //set the download as completed
            processQueue.getBucketYoutube().remove(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
