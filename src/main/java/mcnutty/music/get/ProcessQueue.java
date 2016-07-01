//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

class ProcessQueue {

    ConcurrentLinkedQueue<QueueItem> bucket_queue;
    ConcurrentLinkedQueue<QueueItem> bucket_played;
    ConcurrentLinkedQueue<QueueItem> bucket_youtube;
    HashMap<String, String> alias_map;

    ProcessQueue() {
        //store items which have been queued and items which have been played this bucket
        bucket_queue = new ConcurrentLinkedQueue<>();
        bucket_played = new ConcurrentLinkedQueue<>();
        bucket_youtube = new ConcurrentLinkedQueue<>();
        alias_map = new HashMap<>();
    }

    boolean ip_can_add(String ip) {
        //Check the max number of buckets
        Properties prop = new Properties();
        int max_buckets = 4;
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            if (prop.getProperty("buckets") != null) {
                max_buckets = Integer.parseInt(prop.getProperty("buckets"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
    boolean new_item(QueueItem item) {
        if (ip_can_add(item.ip)) {
            bucket_queue.add(item);
            save_queue();
            return true;
        }
        return false;
    }

    //return the next item in the bucket to be played
    QueueItem next_item() {
        //Return an enpty item if there is nothing to play
        if (bucket_queue.isEmpty()) {
            return new QueueItem();
        }

        //Return the next item in the current bucket
        ArrayList<String> played_ips = bucket_played.stream().map(item -> item.ip).collect(Collectors.toCollection(ArrayList::new));
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
    void set_played(QueueItem item) {
        bucket_queue.remove(item);
        bucket_played.add(item);
        save_queue();
    }

    //remove an item from the queue
    void delete_item(QueueItem item) {
        bucket_queue.remove(item);
        save_queue();
    }

    //convert a ConcurrentLinkedQueue into a JSON array
    JSONArray json_array_list(ConcurrentLinkedQueue<QueueItem> queue) {
        JSONArray output = new JSONArray();
        try {
            for (QueueItem item : queue) {
                JSONObject object = new JSONObject();
                object.put("name", item.real_name);
                object.put("guid", item.disk_name);
                object.put("ip", item.ip);

                if (alias_map.containsKey(item.ip)) {
                    object.put("alias", alias_map.get(item.ip));
                } else {
                    object.put("alias", "");
                }

                output.put(object);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return output;
    }

    //write the queue to disk
    void save_queue(){
        try(PrintWriter file = new PrintWriter("queue.json")){
            file.println(json_array_list(bucket_queue).toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
