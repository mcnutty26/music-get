# music-get

One of the (many) music server implementations for UWCS. While the other options out there may be shinier, music-get is intended to be an easily maintainable alternative prioritising simplicity over extra features. Playing works on a round robin system - each bucket contains at most one item queued by a single IP address. A chrome extension which makes queueing URLs easier is available at [https://github.com/mcnutty26/music-get-autoqueue](https://github.com/mcnutty26/music-get-autoqueue).

##Setup
* Create `config.properties` in `dist`, and optionally have it contain the following key=value pairs:
  * password (the admin password, no default)
  * buckets (the maximum number of tracks queued by each user, default is 4)
  * timeout (the maximum number of seconds a song can play for, default is 547)
  * directory (the location on disk where uploaded songs are stored, default is `/tmp/musicserver/`)
* Create an Apache virtualhost with the document root pointed at the music-get `dist` folder (a sample config is provided in `dist/apache`)
* Enable `mod_rewrite`, `mod_proxy`, and `mod_proxy_html` in Apache (`a2enmod proxy` etc.)
* Run `music.get` (this will pull any repo updates, build the project, update youtube-dl, and start the server back end)

##API:
###The API is exposed on port 8080, and proxied though Apache as /api/ on port 80.
##### /list returns a JSON array representing the current queue 
```curl music.lan/api/list```
##### /last returns a JSON array representing the played items in the current bucket
```curl music.lan/api/last```
##### /current returns a string containing the name of the currently playing item
```curl music.lan/api/current```
##### /add accepts a file as multipart/form-data and adds it to the queue
```curl music.lan/api/add -F "file=@/path/to/file.mp3"```
##### /url accepts a URL as multipart/form-data, downloads the file at that url, and adds it to the queue
```curl music.lan/api/url -F "url=https://www.youtube.com/watch?v=QcIy9NiNbmo"```
##### /downloading returns a JSON array representing the currently downloading videos (submitted to /url)
```curl music.lan/api/downloading```
##### /alias returns 'canalias' or 'cannotalias' depending on whether the requester has an alias set
```curl music.lan/api/alias```
##### /alias/add accepts an alias as multipart/form-data for the requester if they do not have one set
```curl music.lan/api/alias/add -F "alias=myalias"```
##### /remove accepts a guid as multipart/form-data (obtained from /list) and removes the associated item from the queue if the requesting ip queued that item
```curl music.lan/api/remove -F "guid=3c0a7a25-ffc5-4654-8e96-f8dc5dc70f5c"```
##### /admin/kill accepts a password via post and stops the currently playing item
```curl music.lan/api/admin/kill -F "pw=letmein123"```
##### /admin/remove accepts a guid (obtained from /list) and a password as multipart/form-data and removes the associated item from the queue if the password matches the one in `config.properties`.
```curl music.lan/api/admin/remove -F "guid=3c0a7a25-ffc5-4654-8e96-f8dc5dc70f5c" -F "pw=letmein123"```
##### /admin/alias accepts an ip address, a password, and optionally an alias as multipart/form-data and sets (or resets) the alias for that ip if the password matches the one in `config.properties`.
```curl music.lan/api/admin/alias -F "ip=192.168.1.0" [-F "alias=newalias"] -F "pw=letmein123"```
##Dependencies (with links to source code):
* [Flat-UI](https://github.com/designmodo/Flat-UI) (included in the `dist` directory)
* [Jetty](https://github.com/eclipse/jetty.project) 
* [JSON-java](https://github.com/stleary/JSON-java) 
* [youtube-dl](https://github.com/rg3/youtube-dl/) (included in the `dist` directory)
* [JRE 8](http://download.java.net/openjdk/jdk8/)
* [MPlayer](https://www.mplayerhq.hu/design7/dload.html)
* [Apache](https://github.com/apache/httpd)
* [PHP](https://github.com/php/php-src)

##Licenses:
* music-get is distributed under the GNU GPL v3
* [Flat-UI](https://github.com/designmodo/Flat-UI) is licensed under the Creative Commons Attribution 3.0 Unported license (CC BY 3.0)
* [youtube-dl](https://github.com/rg3/youtube-dl/) is public domain
* [Jetty](https://github.com/eclipse/jetty.project) is licenced under the Apache Licence 2.0
* [JSON-java](https://github.com/stleary/JSON-java) is licensed under a bespoke license
