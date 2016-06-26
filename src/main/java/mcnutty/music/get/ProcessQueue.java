//This software is licensed under the GNU GPL v3
//Written by William Seymour and Christopher Leonard

package mcnutty.music.get;

import java.util.ArrayList;

public class ProcessQueue {

    public ArrayList<QueueItem> bucket_queue;
    public ArrayList<QueueItem> bucket_played;
    public ArrayList<QueueItem> bucket_youtube;

    private static final int max_buckets = 4;

    public ProcessQueue() {
        //store items which have been queued and items which have been played this bucket
        bucket_queue = new ArrayList<QueueItem>();
        bucket_played = new ArrayList<QueueItem>();
        bucket_youtube = new ArrayList<QueueItem>();
    }

    public boolean ip_can_add(String ip) {
        //Count items this IP has already queued
        int ip_queued = 0;
        for (QueueItem bucket_item : bucket_queue) {
            if (bucket_item.ip.equals(ip)) {
                ++ip_queued;
                if (ip_queued >= max_buckets) {
                    return false;
                }
            }
        }
        return true;
    }

    //add a new item to the queue, return false if not allowed
    public boolean new_item(QueueItem item) {
        if (ip_can_add(item.ip)) {
            bucket_queue.add(item);
            return true;
        }
        return false;
    }

    //return the next item in the bucket to be played
    public QueueItem next_item() {
        //Return an enpty item if there is nothing to play
        if (bucket_queue.isEmpty()) {
            return new QueueItem();
        }

        //Return the next item in the current bucket
        ArrayList<String> played_ips = new ArrayList<>();
        for (QueueItem item : bucket_played) {
            played_ips.add(item.ip);
        }
        for (QueueItem item : bucket_queue) {
            if (!played_ips.contains(item.ip)) {
                return item;
            }
        }

        //If the current bucket it empty, start the next one
        System.out.println("REACHED END OF BUCKET");
        bucket_played.clear();
        return next_item();
    }

    //move an item from the queue to the played list
    public void set_played(QueueItem item) {
        bucket_queue.remove(item);
        bucket_played.add(item);
    }

    //remove an item from the queue
    public void delete_item(QueueItem item) {
        bucket_queue.remove(item);
    }
}