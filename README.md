# music-get

One of the (many) music server implementations for UWCS. While the other options out there may be shinier, music-get is intended to be an easily maintainable alternative prioritising simplicity over extra features. Playing works on a round robin system - each bucket contains at most one item queued by a single IP.

##Documentation
See the [wiki](https://github.com/mcnutty26/music-get/wiki) for more information on how music-get works.

##Setup
* create config.ini in bin, and have it contain the admin password
* Create an apache virtualhost with the document root pointed at the music-get bin folder
* Add ```Header set Access-Control-Allow-Origin "*"``` to the apache virtualhost
* Run ```java -jar music-get.jar``` in bin

##API:
* /list returns a JSON array representing the current queue
* /last returns a JSON array representing the played items in the current bucket
* /current returns a string containing the name of the currently playing item
* /add accepts a file via post (named file) and adds it to the queue
* /url accepts a URL via post (named url) and attempts to download that file
* /downloading returns a JSON array representing the currently downloading videos from /url
* /remove accepts a guid via post (from /list) and removes that guid from the queue if the requesting ip queued that item
* /admin/kill accepts a password via post and stops the currently playing item
* /admin/remove accepts a guid (from /list) and a password via post and removes that guid from the queue

##Dependencies:
* Jetty (included in music-get.jar)
* JSON-java (included in music-get.jar)
* youtube-dl (included in music-get.jar)
* Java 8
* MPlayer
* Apache
* PHP
