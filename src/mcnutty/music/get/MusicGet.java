package mcnutty.music.get;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class MusicGet {
	
	public static void main( String[] args ) throws Exception
	{    
		int timeout = 547; //timeout in seconds
		String directory = "/musicserver/"; //place in tmp where files will be stored
		
		//print out music-get
		System.out.println("                     _                      _   ");
		System.out.println(" _ __ ___  _   _ ___(_) ___       __ _  ___| |_ ");
		System.out.println("| '_ ` _ \\| | | / __| |/ __|____ / _` |/ _ \\ __|");
		System.out.println("| | | | | | |_| \\__ \\ | (_|_____| (_| |  __/ |_ ");
		System.out.println("|_| |_| |_|\\__,_|___/_|\\___|     \\__, |\\___|\\__|");
		System.out.println("                                 |___/          ");
		
		
		//clean out the directory where music files will go
		try {
			Files.walkFileTree(Paths.get(System.getProperty("java.io.tmpdir") + directory), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
			});
			Files.delete(Paths.get(System.getProperty("java.io.tmpdir") + directory));
		} catch (Exception ex) {
		}
		Files.createDirectory(Paths.get(System.getProperty("java.io.tmpdir") + directory));
		
		//create a queue object
		ProcessQueue process_queue = new ProcessQueue();
		
		//start the web server
		StartServer start_server = new StartServer(process_queue, directory);
		new Thread(start_server).start();
		
		//wit for the web server to spool up
		Thread.sleep(1000);
    
		//read items from the queue and play them
	    while (true) {
	    	QueueItem next_item = process_queue.next_item();
	    	if (!next_item.equals(new QueueItem())) {
	    		System.out.println("Playing " + next_item.real_name);
		    	process_queue.set_played(next_item);
		    	Process p = Runtime.getRuntime().exec("timeout " + timeout + "s mplayer -fs -quiet -af volnorm=2:0.25 "
		    			+ System.getProperty("java.io.tmpdir") + directory + "/" + next_item.disk_name);

		    	try {
		    		p.waitFor();
		    		Files.delete(Paths.get(System.getProperty("java.io.tmpdir") + directory + next_item.disk_name));
		    	} catch (Exception ex) {
		    	}
		    }
	    	Thread.sleep(1000);
		}
	}

}