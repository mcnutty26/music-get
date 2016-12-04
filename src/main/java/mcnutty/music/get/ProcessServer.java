//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ProcessServer extends AbstractHandler {

    private ProcessQueue process_queue;
    private String directory;

    ProcessServer(ProcessQueue process_queue, String directory) {
        this.process_queue = process_queue;
        this.directory = directory;
    }

    //all requests are routed through this method
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {

        boolean isMultipart = false;
        if (request.getContentType() != null && (request.getContentType().startsWith("multipart/form-data")
                || request.getContentType().startsWith("application/x-www-form-urlencoded"))) {
            baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
            isMultipart = true;
        }

        response.setContentType("text/html; charset=utf-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        //select which endpoint the user requested
        try {
            switch (target) {
                case "/list":
                    list(out);
                    break;
                case "/last":
                    last(out);
                    break;
                case "/current":
                    current(out);
                    break;
                case "/add":
                    if (isMultipart) {
                        add(request, response);
                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected multipart/form-data.");
                    }
                    break;
                case "/url":
                    if (isMultipart) {
                        url(request, response);
                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected multipart/form-data.");
                    }
                    break;
                case "/downloading":
                    downloading(out);
                case "/remove":
                    remove(request);
                    break;
                case "/alias/add":
                    alias(request, response);
                    break;
                case "/alias":
                    can_alias(request, out);
                    break;
                case "/admin/kill":
                    kill(request);
                    break;
                case "/admin/remove":
                    admin_remove(request, response);
                    break;
                case "/admin/alias":
                    admin_alias(request, response);
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Requested endpoint does not exist.");
            }
        } catch (IOException | ServletException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
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

    //list the currently queued items
    private void list(PrintWriter out) {
        out.println(process_queue.json_array_list(process_queue.bucket_queue));
    }

    //list the name of the currently playing item 
    private void current(PrintWriter out) {
        QueueItem item = new QueueItem();
        for (QueueItem lastItem: process_queue.bucket_played) item = lastItem;
        ConcurrentLinkedQueue<QueueItem> currently_playing = new ConcurrentLinkedQueue<>();
        currently_playing.add(item);
        out.println(process_queue.json_array_list(currently_playing));
    }

    //list the items which have been played this bucket
    private void last(PrintWriter out) {
        out.println(process_queue.json_array_list(process_queue.bucket_played));
    }

    //add an uploaded file to the queue 
    private void add(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String guid = UUID.randomUUID().toString();
        Part uploaded_file;
        uploaded_file = request.getPart("file");
        if (uploaded_file == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file provided.");
        } else {
            uploaded_file.write(directory + guid);
            QueueItem new_item = new QueueItem(guid, extractFileName(uploaded_file), request.getRemoteAddr());
            if (!new_item.real_name.equals("")) {
                if (process_queue.new_item(new_item)) {
                    System.out.println(new_item.ip + " added file " + new_item.real_name);
                } else {
                    System.out.println(new_item.ip + " rejected file " + new_item.real_name);
                    Files.delete(Paths.get(directory + new_item.disk_name));
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Client has too many items queued");
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file provided");
            }
        }
    }

    //download a video at the supplied URL
    private void url(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String param = request.getParameter("url");
        if (param == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No URL provided.");
        } else if (!process_queue.ip_can_add(request.getRemoteAddr())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Client has too many items queued.");
        } else {
            if (!request.getParameter("url").equals("")) {
                YoutubeDownload download = new YoutubeDownload(request.getParameter("url"), process_queue, request.getRemoteAddr(), directory);
                ExecutorService executor = Executors.newCachedThreadPool();
                executor.submit(download);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty URL provided.");
            }
        }
    }

    //list the currently downloading items
    private void downloading(PrintWriter out) {
        out.println(process_queue.json_array_list(process_queue.bucket_youtube));
    }

    //remove an item from the queue
    private void remove(HttpServletRequest request) {
        QueueItem match = null;
        for (QueueItem item : process_queue.bucket_queue) {
            if (item.disk_name.equals(request.getParameter("guid")) && item.ip.equals(request.getRemoteAddr())) {
                match = item;
            }
        }
        if (match != null) {
            process_queue.delete_item(match);
            System.out.println(match.ip + " deleted item " + match.real_name);
        }
    }

    //admin: kill the currently playing item
    private void kill(HttpServletRequest request) throws IOException {
        if (auth(request.getParameter("pw"))) {
            Runtime.getRuntime().exec("killall mplayer");

            if (!process_queue.bucket_played.isEmpty()) {
                QueueItem last = new QueueItem();
                for (QueueItem item: process_queue.bucket_played) {
                    last = item;
                }
                System.out.println(request.getRemoteAddr() + " (admin) killed item " + last.real_name + " from " + last.ip);
            }
        }
    }

    //admin: remove any item from the queue
    private void admin_remove(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (auth(request.getParameter("pw"))) {
            QueueItem match = null;
            for (QueueItem item : process_queue.bucket_queue) {
                if (item.disk_name.equals(request.getParameter("guid"))) {
                    match = item;
                }
            }
            if (match != null) {
                process_queue.delete_item(match);
                System.out.println(request.getRemoteAddr() + " (admin) removed item " + match.real_name + " queued by " + match.ip);
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not authenticated.");
        }
    }

    //add an alias for the requester if they don't already have one
    private void alias(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getParameter("alias").equals("")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty alias provided.");
        } else if (can_alias(request)) {
            process_queue.alias_map.put(request.getRemoteAddr(), request.getParameter("alias"));
            System.out.println(request.getRemoteAddr() + " added alias " + process_queue.alias_map.get(request.getRemoteAddr()));
        } else {
            System.out.println(request.getRemoteAddr() + " had new alias rejected (they already have one)");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User already has an alias set.");
        }
    }

    //return true if the requester has an alias set
    private boolean can_alias(HttpServletRequest request) {
        return !process_queue.alias_map.containsKey(request.getRemoteAddr());
    }

    //let the requester know if they have an alias set
    private void can_alias(HttpServletRequest request, PrintWriter out) {
        if (process_queue.alias_map.containsKey(request.getRemoteAddr())) {
            out.print("cannotalias");
        } else {
            out.print("canalias");
        }
    }

    //admin: force unset or force change the alias of a user
    private void admin_alias(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (auth(request.getParameter("pw"))) {
            if (request.getParameter("alias").equals("")) {
                process_queue.alias_map.remove(request.getParameter("ip"));
                System.out.println(request.getRemoteAddr() + " removed alias for " + request.getParameter("ip"));
            } else {
                process_queue.alias_map.replace(request.getParameter("ip"), request.getParameter("alias"));
                System.out.println(request.getRemoteAddr() + " (admin) changed alias for " + request.getParameter("ip") + " to " + request.getParameter("alias"));
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not authenticated");
        }
    }

    private boolean auth(String user_password) throws IOException {
        Properties prop = new Properties();
        InputStream input;
        input = new FileInputStream("config.properties");
        prop.load(input);
        return prop.getProperty("password").equals(user_password);
    }
}
