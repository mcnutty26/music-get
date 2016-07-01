//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import org.eclipse.jetty.server.Server;

class StartServer implements Runnable {

    private ProcessQueue process_queue;
    private String directory;

    StartServer(ProcessQueue process_queue, String directory) {
        this.process_queue = process_queue;
        this.directory = directory;
    }

    @Override
    public void run() {
        Server server = new Server(8080);
        server.setHandler(new ProcessServer(process_queue, directory));
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
