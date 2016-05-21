//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.util.ArrayList;
import java.util.stream.Collectors;

class ProcessQueue {

    private ArrayList<QueueItem> bucketQueue;
    private ArrayList<QueueItem> bucketPlayed;
    private ArrayList<QueueItem> bucketYoutube;

    ProcessQueue() {
        //store items which have been queued and items which have been played this bucket
        bucketQueue = new ArrayList<>();
        bucketPlayed = new ArrayList<>();
        bucketYoutube = new ArrayList<>();
    }

    ArrayList<QueueItem> getBucketQueue() {
        return bucketQueue;
    }

    ArrayList<QueueItem> getBucketPlayed() {
        return bucketPlayed;
    }

    ArrayList<QueueItem> getBucketYoutube() {
        return bucketYoutube;
    }

    //add a new item to the queue
    void newItem(QueueItem item) {
        bucketQueue.add(item);
    }

    //return the next item in the bucket to be played
    QueueItem nextItem() {
        if (!bucketQueue.isEmpty()) {
            ArrayList<String> playedIps = bucketPlayed.stream()
                    .map(QueueItem::getIp)
                    .collect(Collectors.toCollection(ArrayList::new));
            for (QueueItem item : bucketQueue) {
                if (!playedIps.contains(item.getIp())) {
                    return item;
                }
            }
            System.out.println("REACHED END OF BUCKET");
            bucketPlayed.clear();
            return nextItem();
        }
        return new QueueItem();
    }

    //move an item from the queue to the played list
    void setPlayed(QueueItem item) {
        bucketQueue.remove(item);
        bucketPlayed.add(item);
    }

    //remove an item from the queue
    void deleteItem(QueueItem item) {
        bucketQueue.remove(item);
    }
}
