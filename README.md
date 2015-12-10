# music-get

One of the (many) music server implementations for UWCS. While the other options out there may be shinier, music-get is intended to be a lightweight alternative prioritising simplicity over extra features. Playing works on a round robin system - each ip address gets something played before anyone can have something played twice. music-get runs on port 8080, and files are stored in /tmp/musicserver/.

##To start: java -jar music-get.jar

##API:
* /list returns a JSON array representing the current queue
* /played returns a JSON array representing the played items in the current bucket
* /add accepts a file via post (named file)
* /remove accepts a guid via post (from /list) and removes that guid from the queue if the requesting ip queued that item

##Dependencies:
* jetty (included)
* JSON-java (included)
* mplayer
