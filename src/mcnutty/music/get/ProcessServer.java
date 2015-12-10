package mcnutty.music.get;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

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
        //response.addHeader("Access-Control-Allow-Methods", "POST");
        response.setStatus(HttpServletResponse.SC_OK);
 
        PrintWriter out = response.getWriter();
        
        switch(target) {
        case "/list":
        	list(out);
        	break;
        case "/current":
        	current(out);
        	break;
        case "/add":
        	add(request);
        	redirect(request, response);
        	break;
        case "/remove":
        	remove(request);
        default:
        	out.println("Welcome to the music-get backend!");
        }
 
        baseRequest.setHandled(true);
    }
    
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
    
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
		} finally {
    	}
    	return output;
    }
    
    void list(PrintWriter out) {
    	out.println(json_array_list(process_queue.bucket_queue));
    }
    
    void current(PrintWriter out) {
    	try {
    		out.println(process_queue.bucket_played.get(process_queue.bucket_played.size() - 1).real_name);
    	} catch (Exception e) {
    		out.println("Nothing!");
    	}
    }
    
    void last(PrintWriter out) {
    	out.println(json_array_list(process_queue.bucket_played));
    }
    
    void add(HttpServletRequest request) {
    	String guid = UUID.randomUUID().toString();
    	Part uploaded_file;
		try {
			uploaded_file = request.getPart("file");;
			uploaded_file.write(directory + guid);
			QueueItem new_item = new QueueItem(guid, extractFileName(uploaded_file), request.getRemoteAddr());
	    	process_queue.new_item(new_item);
	    	System.out.println("Added file " + new_item.real_name + " from " + new_item.ip);
		} catch (IOException | ServletException e) {
			e.printStackTrace();
		}
    }
    
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
    
    void redirect(HttpServletRequest request, HttpServletResponse response) {
    	String ip = request.getLocalAddr();
    	String url = "http://" + ip + "/index.php";
    	response.setContentLength(0);
    	try {
			response.sendRedirect(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
}

