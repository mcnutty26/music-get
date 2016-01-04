package mcnutty.music.get;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;
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

public class ProcessServer extends AbstractHandler {
	
	ProcessQueue process_queue;
	String directory;
	
	public ProcessServer(ProcessQueue process_queue, String directory) {
		this.process_queue = process_queue;
		this.directory = directory;
	}
 
	//all requests are routed through this method
    public void handle( String target,
                        Request baseRequest,
                        HttpServletRequest request,
                        HttpServletResponse response ) throws IOException,
                                                      ServletException
    {
    	if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
  		  baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
  		}
    	
        response.setContentType("text/html; charset=utf-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setStatus(HttpServletResponse.SC_OK);
 
        PrintWriter out = response.getWriter();
        
        //select which endpoint the user requested
        switch(target) {
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
        	redirect(request, response);
        	break;
        case "/url":
        	url(request);
        	redirect(request, response);
        	break;
        case "/downloading":
        	downloading(out);
        case "/remove":
        	remove(request);
        	break;
        case "/admin/kill":
        	kill(request);
        	break;
        case "/admin/remove":
        	admin_remove(request);
        	break;
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
                return s.substring(s.indexOf("=") + 2, s.length()-1);
            }
        }
        return "";
    }
    
    //convert an ArrayList into a JSON array
    JSONArray json_array_list(ArrayList<QueueItem> queue) {
    	JSONArray output = new JSONArray();
    	try {
	    	for (QueueItem item : queue) {
	    		JSONObject object = new JSONObject();
	    		object.put("name", item.real_name);
	    		object.put("guid", item.disk_name);
	    		object.put("ip", item.ip);
	    		output.put(object);
	    	}
    	} catch (JSONException e) {
			e.printStackTrace();
		}
    	return output;
    }
    
    //list the currently queued items
    void list(PrintWriter out) {
    	out.println(json_array_list(process_queue.bucket_queue));
    }
    
    //list the name of the currently playing item 
    void current(PrintWriter out) {
    	try {
    		out.println(process_queue.bucket_played.get(process_queue.bucket_played.size() - 1).real_name);
    	} catch (Exception e) {
    		out.println("nothing!");
    	}
    }
    
    //list the items which have been played this bucket
    void last(PrintWriter out) {
    	out.println(json_array_list(process_queue.bucket_played));
    }
    
    //add an uploaded file to the queue 
    void add(HttpServletRequest request) {
    	String guid = UUID.randomUUID().toString();
    	Part uploaded_file;
		try {
			uploaded_file = request.getPart("file");;
			uploaded_file.write(directory + guid);
			QueueItem new_item = new QueueItem(guid, extractFileName(uploaded_file), request.getRemoteAddr());
			if (!new_item.real_name.equals("")) {
				process_queue.new_item(new_item);
				System.out.println("Added file " + new_item.real_name + " from " + new_item.ip);
			}
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}
    }
    
    //download a video at the supplied URL
    void url(HttpServletRequest request) {
    	if (!request.getParameter("url").equals("")) {
    		YoutubeDownload download = new YoutubeDownload(request.getParameter("url"), process_queue, request.getRemoteAddr(), directory);
    		ExecutorService executor = Executors.newCachedThreadPool();
    		executor.submit(download);
    	}
    }
    
    //list the currently downloading items
    void downloading(PrintWriter out) {
    	out.println(json_array_list(process_queue.bucket_youtube));
    }
    
    //remove an item from the queue
    void remove(HttpServletRequest request) {
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
    void redirect(HttpServletRequest request, HttpServletResponse response) {
    	String ip = request.getLocalName();
    	System.out.println(request.getLocalName());
    	String url = "http://" + ip + "/index.php";
    	response.setContentLength(0);
    	try {
			response.sendRedirect(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    //admin: kill the currently playing item
    void kill(HttpServletRequest request) {
    	BufferedReader br;
    	String pw = "";
		try {
			br = new BufferedReader(new FileReader("config.ini"));
			pw = br.readLine();
			if (request.getParameter("pw").equals(pw)) {
	    			Runtime.getRuntime().exec("killall mplayer");
	    	}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    //admin: remove any item from the queue
    void admin_remove(HttpServletRequest request) {
    	QueueItem match = null;
    	for (QueueItem item : process_queue.bucket_queue) {
			if (item.disk_name.equals(request.getParameter("guid"))) {
				match = item;
			}
    	}
    	BufferedReader br;
    	String pw = "";
		try {
			br = new BufferedReader(new FileReader("config.ini"));
			pw = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	if (match != null && request.getParameter("pw").equals(pw)) {
    		process_queue.delete_item(match);
    	}
    }
    
}

