//This software is licensed under the GNU GPL v3
//Written by William Seymour

package mcnutty.music.get;

import org.eclipse.jetty.server.Server;

class StartServer implements Runnable {

    private ProcessQueue processQueue;
    private String directory;

    StartServer(ProcessQueue processQueue, String directory) {
        this.processQueue = processQueue;
        this.directory = directory;
    }

    @Override
    public void run() {
        Server server = new Server(8080);
        server.setHandler(new ProcessServer(processQueue, directory));
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
