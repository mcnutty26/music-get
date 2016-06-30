//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ProcessServer extends AbstractHandler {

    private ProcessQueue process_queue;
    private String directory;
    private HashMap<String, String> alias_map;

    ProcessServer(ProcessQueue process_queue, String directory) {
        this.process_queue = process_queue;
        this.directory = directory;
        this.alias_map = new HashMap<String, String>();
    }

    //all requests are routed through this method
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
            ServletException {
        if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
            baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
        }

        response.setContentType("text/html; charset=utf-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        //select which endpoint the user requested
        switch (target) {
            case "/list":
                list(request, out);
                break;
            case "/last":
                last(request, out);
                break;
            case "/current":
                current(out);
                break;
            case "/add":
                if (add(request)) {
                    redirect(request, response, "index", "");
                } else {
                    redirect(request, response, "index", "error=limit");
                }
                break;
            case "/url":
                if (process_queue.ip_can_add(request.getRemoteAddr())) {
                    url(request);
                    redirect(request, response, "index", "");
                } else {
                    redirect(request, response, "index", "error=limit");
                }
                break;
            case "/downloading":
                downloading(request, out);
            case "/remove":
                remove(request);
                break;
            case "/alias/add":
                alias(request);
                redirect(request, response, "index", "");
                break;
            case "/alias":
                can_alias(request, out);
                break;
            case "/admin/kill":
                kill(request);
                break;
            case "/admin/remove":
                admin_remove(request);
                break;
            case "/admin/alias":
                admin_alias(request);
                redirect(request, response, "admin", "");
            default:
                out.println("Welcome to the music-get backend!");
        }

        baseRequest.setHandled(true);
    }

    //allow multi part forms (required for file uploads)
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("/");

    //get the name of an uploaded file
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }

    //convert an ArrayList into a JSON array
    private JSONArray json_array_list(ConcurrentLinkedQueue<QueueItem> queue, HttpServletRequest request) {
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

    //list the currently queued items
    private void list(HttpServletRequest request, PrintWriter out) {
        out.println(json_array_list(process_queue.bucket_queue, request));
    }

    //list the name of the currently playing item 
    private void current(PrintWriter out) {
        QueueItem item = new QueueItem();
        for (QueueItem lastItem: process_queue.bucket_played) item = lastItem;
        if (item.equals(new QueueItem())) {
            out.println("nothing!");
        } else{
            String display_name = item.ip;
            if (alias_map.containsKey(item.ip)) {
                display_name = alias_map.get(item.ip);
            }
            out.println((item.real_name.length() > 61 ? item.real_name.substring(0, 60) : item.real_name) + " by " + (display_name.length() > 21 ? display_name.substring(0, 20) : display_name));
        }
    }

    //list the items which have been played this bucket
    private void last(HttpServletRequest request, PrintWriter out) {
        out.println(json_array_list(process_queue.bucket_played, request));
    }

    //add an uploaded file to the queue 
    private boolean add(HttpServletRequest request) {
        String guid = UUID.randomUUID().toString();
        Part uploaded_file;
        boolean added_file = false;
        try {
            uploaded_file = request.getPart("file");
            ;
            uploaded_file.write(directory + guid);
            QueueItem new_item = new QueueItem(guid, extractFileName(uploaded_file), request.getRemoteAddr());
            if (!new_item.real_name.equals("")) {
                added_file = process_queue.new_item(new_item);
                if (added_file) {
                    System.out.println("Added file " + new_item.real_name + " from " + new_item.ip);
                } else {
                    System.out.println("Rejected file " + new_item.real_name + " from " + new_item.ip + " - too many items queued");
                    try {
                        Files.delete(Paths.get(directory + new_item.disk_name));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException | ServletException e) {
            e.printStackTrace();
            return false;
        }
        return added_file;
    }

    //download a video at the supplied URL
    private void url(HttpServletRequest request) {
        if (!request.getParameter("url").equals("")) {
            YoutubeDownload download = new YoutubeDownload(request.getParameter("url"), process_queue, request.getRemoteAddr(), directory);
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(download);
        }
    }

    //list the currently downloading items
    private void downloading(HttpServletRequest request, PrintWriter out) {
        out.println(json_array_list(process_queue.bucket_youtube, request));
    }

    //remove an item from the queue
    private void remove(HttpServletRequest request) {
        QueueItem match = null;
        for (QueueItem item : process_queue.bucket_queue) {
            if (item.disk_name.equals(request.getParameter("guid")) && item.ip.equals(request.getRemoteAddr().toString())) {
                match = item;
            }
        }
        if (match != null) {
            process_queue.delete_item(match);
            System.out.println(match.ip + " deleted item " + match.real_name);
        }
    }

    //redirect the requester back to the front end
    private void redirect(HttpServletRequest request, HttpServletResponse response, String page, String args) {
        String name = "music.lan";
		String addon = "";
		if (!args.equals("")){
			addon = "?" + args;
		}
        String url = "http://" + name + "/" + page + ".php" + addon;
        response.setContentLength(0);
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //admin: kill the currently playing item
    private void kill(HttpServletRequest request) {
        if (auth(request.getParameter("pw"))) {
            try {
                Runtime.getRuntime().exec("killall mplayer");
                System.out.println("Current item killed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //admin: remove any item from the queue
    private void admin_remove(HttpServletRequest request) {
        QueueItem match = null;
        for (QueueItem item : process_queue.bucket_queue) {
            if (item.disk_name.equals(request.getParameter("guid"))) {
                match = item;
            }
        }
        if (auth(request.getParameter("pw"))) {
            process_queue.delete_item(match);
            System.out.println("Item " + match.real_name + " removed from queue by admin");
        }
    }

    //add an alias for the requester if they don't already have one
    private void alias(HttpServletRequest request) {
        if (can_alias(request) && !request.getParameter("alias").equals("")) {
            alias_map.put(request.getRemoteAddr(), request.getParameter("alias"));
            System.out.println("Added alias " + alias_map.get(request.getRemoteAddr()) + " for user at " + request.getRemoteAddr());
        } else {
            System.out.println("Rejected alias from " + request.getRemoteAddr() + " - user already has an alias");
        }
    }

    //return true if the requester has an alias set
    private boolean can_alias(HttpServletRequest request) {
        return !alias_map.containsKey(request.getParameter("ip"));
    }

    //let the requester know if they have an alias set
    private void can_alias(HttpServletRequest request, PrintWriter out) {
        if (alias_map.containsKey(request.getParameter("ip"))) {
            out.print("cannotalias");
        } else {
            out.print("canalias");
        }
    }

    //admin: force unset or force change the alias of a user
    private void admin_alias(HttpServletRequest request) {
        if (auth(request.getParameter("pw"))) {
            if (request.getParameter("alias").equals("")) {
                alias_map.remove(request.getParameter("ip"));
                System.out.println("Alias reset for " + request.getParameter("ip"));
            } else {
                alias_map.replace(request.getParameter("ip"), request.getParameter("alias"));
                System.out.println("Alias changed for " + request.getParameter("ip"));
            }
        }
    }

    private boolean auth(String user_password) {
        Properties prop = new Properties();
        InputStream input;
        try {
            input = new FileInputStream("config.properties");
            prop.load(input);
            if (prop.getProperty("password").equals(user_password)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
