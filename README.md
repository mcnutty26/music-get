# music-get

One of the (many) music server implementations for UWCS. While the other options out there may be shinier, music-get is intended to be a lightweight alternative prioritising simplicity over extra features. Playing works on a round robin system - each ip address gets something played before anyone can have something played twice. 

##Setup
* Create an apache virtualhost with the document root pointed at the music-get bin folder
* Add ```Header set Access-Control-Allow-Origin "*"``` to the apache virtualhost
* Run ```java -jar music-get.jar```
The back end will bind to port 8080, and files will be stored in /tmp/musicserver/.

##API:
* /list returns a JSON array representing the current queue
* /current returns a string containing the name of the currently playing item
* /last returns a JSON array representing the played items in the current bucket
* /add accepts a file via post and adds it to the queue (named file)
* /remove accepts a guid via post (from /list) and removes that guid from the queue if the requesting ip queued that item

##Dependencies:
* jetty (included)
* JSON-java (included)
* mplayer
* apache
* PHP
