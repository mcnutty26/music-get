# music-get

One of the (many) music server implementations for UWCS. While the other options out there may be shinier, music-get is intended to be an easily maintainable alternative prioritising simplicity over extra features. Playing works on a round robin system - each bucket contains at most one item queued by a single IP.

##Documentation
See the [wiki](https://github.com/mcnutty26/music-get/wiki) for more information on how music-get works.

##Setup
* create `config.properties` in `dist`, and optionally have it contain the following key=value pairs:
  * password (the admin password, no default)
  * buckets (the maximum number of tracks queued by each user, default is 4)
  * timeout (the maximum number of seconds a song can play for, default is 547)
  * directory (the location on disk where uploaded songs are stored, default is `/tmp/musicserver/`)
* Create an apache virtualhost with the document root pointed at the music-get `dist` folder (a sample config is provided in `dist/apache`)
* Run `music.get` (this will pull any repo updates, build the project, upgrade youtube-dl, and start the server back end)

##API:
##### /list returns a JSON array representing the current queue 
```curl music:8080/list```
##### /last returns a JSON array representing the played items in the current bucket
```curl music:8080/last```
##### /current returns a string containing the name of the currently playing item
```curl music:8080/current```
##### /add accepts a file via post and adds it to the queue
```curl music:8080/add -F "file=@/home/bob/path/to/file.mp3"```
##### /url accepts a URL via post and attempts to download that file
```curl music:8080/url -F "url=https://www.youtube.com/watch?v=QcIy9NiNbmo"```
##### /downloading returns a JSON array representing the currently downloading videos from /url
```curl music:8080/downloading```
##### /alias returns 'canalias' or 'cannotalias' depending on whether the requester has an alias set
```curl music:8080/alias```
##### /alias/add adds an alias for the requester if they do not have one set
```curl music:8080/alias/add -F "alias=myalias"```
##### /remove accepts a guid via post (from /list) and removes that guid from the queue if the requesting ip queued that item
```curl music:8080/remove -F "guid=3c0a7a25-ffc5-4654-8e96-f8dc5dc70f5c"```
##### /admin/kill accepts a password via post and stops the currently playing item
```curl music:8080/admin/kill -F "pw=letmein123"```
##### /admin/remove accepts a guid (from /list) and a password via post and removes that guid from the queue
```curl music:8080/admin/remove -F "guid=3c0a7a25-ffc5-4654-8e96-f8dc5dc70f5c" -F "pw=letmein123"```
##### /admin/alias accepts an ip address, a password, and optionally an alias via post and sets (or resets) the alias for that ip
```curl music:8080/admin/alias -F "ip=192.168.1.0" [-F "alias=newalias"] -F "pw=letmein123"```
##Dependencies (with links to source code):
* [Flat-UI](https://github.com/designmodo/Flat-UI) (included in the `dist` directory)
* [Jetty](https://github.com/eclipse/jetty.project) 
* [JSON-java](https://github.com/stleary/JSON-java) 
* [youtube-dl](https://github.com/rg3/youtube-dl/) (included in the `dist` directory)
* [Java 8](http://download.java.net/openjdk/jdk8/)
* [MPlayer](https://www.mplayerhq.hu/design7/dload.html)
* [Apache](https://github.com/apache/httpd)
* [PHP](https://github.com/php/php-src)

##Licenses:
* music-get is licensed under the GNU GPL v3
* [Flat-UI](https://github.com/designmodo/Flat-UI) is licensed under the Creative Commons Attribution 3.0 Unported license (CC BY 3.0)
* [youtube-dl](https://github.com/rg3/youtube-dl/) is public domain
* [Jetty](https://github.com/eclipse/jetty.project) is licenced under the Apache Licence 2.0
* [JSON-java](https://github.com/stleary/JSON-java) is licensed under a bespoke license
