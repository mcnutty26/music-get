//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ProcessServer extends AbstractHandler {

    private ProcessQueue processQueue;
    private String directory;
    private HashMap<String, String> aliasMap;

    ProcessServer(ProcessQueue processQueue, String directory) {
        this.processQueue = processQueue;
        this.directory = directory;
        this.aliasMap = new HashMap<>();
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
                list(out);
                break;
            case "/last":
                last(out);
                break;
            case "/current":
                current(out);
                break;
            case "/add":
                add(request);
                redirect(response, "index");
                break;
            case "/url":
                url(request);
                redirect(response, "index");
                break;
            case "/downloading":
                downloading(out);
            case "/remove":
                remove(request);
                break;
            case "/alias/add":
                alias(request);
                redirect(response, "index");
                break;
            case "/alias":
                isAliased(request, out);
                break;
            case "/admin/kill":
                kill(request);
                break;
            case "/admin/remove":
                adminRemove(request);
                break;
            case "/admin/alias":
                adminAlias(request);
                redirect(response, "admin");
            default:
                out.println("Welcome to the music-get backend!");
        }

        baseRequest.setHandled(true);
    }

    //allow multi part forms (required for file uploads)
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

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
    private JSONArray jsonArrayList(ArrayList<QueueItem> queue) {
        JSONArray output = new JSONArray();
        try {
            for (QueueItem item : queue) {
                JSONObject object = new JSONObject();
                object.put("name", item.getRealName());
                object.put("guid", item.getDiskName());
                object.put("ip", item.getIp());

                if (aliasMap.containsKey(item.getIp())) {
                    object.put("alias", aliasMap.get(item.getIp()));
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
    private void list(PrintWriter out) {
        out.println(jsonArrayList(processQueue.getBucketQueue()));
    }

    //list the name of the currently playing item 
    private void current(PrintWriter out) {
        try {
            QueueItem item = processQueue.getBucketPlayed().get(processQueue.getBucketPlayed().size() - 1);
            String display_name = item.getIp();
            if (aliasMap.containsKey(item.getIp())) {
                display_name = aliasMap.get(item.getIp());
            }
            out.println(item.getRealName() + " by " + display_name);
        } catch (Exception e) {
            out.println("nothing!");
        }
    }

    //list the items which have been played this bucket
    private void last(PrintWriter out) {
        out.println(jsonArrayList(processQueue.getBucketPlayed()));
    }

    //add an uploaded file to the queue 
    private void add(HttpServletRequest request) {
        String guid = UUID.randomUUID().toString();
        Part uploaded_file;
        try {
            uploaded_file = request.getPart("file");
            uploaded_file.write(directory + guid);
            QueueItem new_item = new QueueItem(guid, extractFileName(uploaded_file), request.getRemoteAddr());
            if (!new_item.getRealName().equals("")) {
                processQueue.newItem(new_item);
                System.out.println("Added file " + new_item.getRealName() + " from " + new_item.getIp());
            }
        } catch (IOException | ServletException e) {
            e.printStackTrace();
        }
    }

    //download a video at the supplied URL
    private void url(HttpServletRequest request) {
        if (!request.getParameter("url").equals("")) {
            YoutubeDownload download = new YoutubeDownload(request.getParameter("url"), processQueue, request.getRemoteAddr(), directory);
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(download);
        }
    }

    //list the currently downloading items
    private void downloading(PrintWriter out) {
        out.println(jsonArrayList(processQueue.getBucketYoutube()));
    }

    //remove an item from the queue
    private void remove(HttpServletRequest request) {
        QueueItem match = null;
        for (QueueItem item : processQueue.getBucketQueue()) {
            if (item.getDiskName().equals(request.getParameter("guid")) && item.getIp().equals(request.getRemoteAddr())) {
                match = item;
            }
        }
        if (match != null) {
            processQueue.deleteItem(match);
            System.out.println(match.getIp() + " deleted item " + match.getRealName());
        }
    }

    //redirect the requester back to the front end
    private void redirect(HttpServletResponse response, String page) {
        String name = "music.lan"; //request.getLocalAddr();
        String url = "http://" + name + "/" + page + ".php";
        response.setContentLength(0);
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //admin: kill the currently playing item
    private void kill(HttpServletRequest request) {
        if (configAuth(request.getParameter("pw"))) {
            try {
                Runtime.getRuntime().exec("killall mplayer");
                System.out.println("Current item killed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //admin: remove any item from the queue
    private void adminRemove(HttpServletRequest request) {
        QueueItem match = null;
        for (QueueItem item : processQueue.getBucketQueue()) {
            if (item.getDiskName().equals(request.getParameter("guid"))) {
                match = item;
            }
        }
        if (configAuth(request.getParameter("pw")) && match != null) {
            processQueue.deleteItem(match);
            System.out.println("Item " + match.getRealName() + " removed from queue by admin");
        }
    }

    //add an alias for the requester if they don't already have one
    private void alias(HttpServletRequest request) {
        if (isAliased(request) && !request.getParameter("alias").equals("")) {
            aliasMap.put(request.getRemoteAddr(), request.getParameter("alias"));
            System.out.println("Added alias " + aliasMap.get(request.getRemoteAddr()) + " for user at " + request.getRemoteAddr());
        } else {
            System.out.println("Attempted double alias for " + request.getRemoteAddr());
        }
    }

    //return true if the requester has an alias set
    private boolean isAliased(HttpServletRequest request) {
        return !aliasMap.containsKey(request.getParameter("ip"));
    }

    //let the requester know if they have an alias set
    private void isAliased(HttpServletRequest request, PrintWriter out) {
        if (aliasMap.containsKey(request.getParameter("ip"))) {
            out.print("cannotalias");
        } else {
            out.print("isAliased");
        }
    }

    //admin: force unset or force change the alias of a user
    private void adminAlias(HttpServletRequest request) {
        if (configAuth(request.getParameter("pw"))) {
            if (request.getParameter("alias").equals("")) {
                aliasMap.remove(request.getParameter("ip"));
                System.out.println("Alias reset for " + request.getParameter("ip"));
            } else {
                aliasMap.replace(request.getParameter("ip"), request.getParameter("alias"));
                System.out.println("Alias changed for " + request.getParameter("ip"));
            }
        }
    }

    private boolean configAuth(String password) {
        BufferedReader br;
        String config_password = "";

        try {
            br = new BufferedReader(new FileReader("config.ini"));
            config_password = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config_password.equals(password);
    }
}