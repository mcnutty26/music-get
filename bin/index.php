<?php
    $server_ip = $_SERVER['SERVER_ADDR'];
    $client_ip = $_SERVER['REMOTE_ADDR']
?>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>music-get</title>
    <meta name="description" content="So you can play music at LAN!"/>

    <meta name="viewport" content="width=1000, initial-scale=1.0, maximum-scale=1.0">

    <!-- Loading Bootstrap -->
    <link href="dist/css/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <!-- Loading Flat UI -->
    <link href="dist/css/flat-ui.css" rel="stylesheet">
    <link href="docs/assets/css/demo.css" rel="stylesheet">
    <link href="dist/css/music-get.css" rel="stylesheet">

    <link rel="shortcut icon" href="img/favicon.ico">

    <!-- HTML5 shim, for IE6-8 support of HTML5 elements. All other JS at the end of file. -->
    <!--[if lt IE 9]>
      <script src="dist/js/vendor/html5shiv.js"></script>
      <script src="dist/js/vendor/respond.min.js"></script>
    <![endif]-->
  </head>
  <body>
    <div class="container">
      <div class="row demo-row">
        <div class="col-xs-12">
          <nav class="navbar navbar-inverse navbar-embossed" role="navigation">
            <div class="navbar-header">
              <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#navbar-collapse-01">
                <span class="sr-only">Toggle navigation</span>
              </button>
              <a class="navbar-brand" href="index.php">music-get</a>
            </div>
            <div class="collapse navbar-collapse" id="navbar-collapse-01">
              <ul class="nav navbar-nav navbar-left">
                <li><a href="index.php">Queue</a></li>
                <li><a href="about.html">About</a></li>
            </div><!-- /.navbar-collapse -->
          </nav><!-- /navbar -->
        </div>
      </div> <!-- /row -->

      <div class="demo-type-example">
        <h3>Bringing you LAN classics since 2015</h3>
      </div>

      <div class="login-form">
        <form action="http://<?=$server_ip?>:8080/add" method="post" enctype="multipart/form-data">
          <div class="row">
            <div class="col-xs-6">
              <div class="form-group">
                <input type="file" class="form-control" name="file"/>
              </div>
            </div>
            <div class="col-xs-6">
              <div class="form-group">
                <input type="submit" class="btn btn-primary btn-lg btn-block">
              </div>
            </div>
          </div>
        </form>
        <div class="form-group">
        
        <h6>Currently playing <?=file_get_contents("http://localhost:8080/current");?></h6>
        
        <table>
            <tr>
              <td></td>
              <td></td>
              <td></td>
            </tr>
        <?php
            $json = file_get_contents("http://localhost:8080/list");
            $data = json_decode($json, true);
            foreach ($data as $item) {
                echo "<tr>";
                echo "<td>" . $item['name'] . "</td>";
                echo "<td>" . $item['ip'] . "</td>";
                $guid = $item['guid'];
                echo "<td>" . ($client_ip == $item['ip'] ? "<a class=\"fui-cross ajax-button\" onclick=\"remove_item('$guid')\"></a>" : "") . "</td>";
                echo "</tr>";
            } ?>
        </div>
      </div>

    </div> <!-- /container -->

    <script src="dist/js/vendor/jquery.min.js"></script>
    <script src="dist/js/vendor/video.js"></script>
    <script src="dist/js/flat-ui.min.js"></script>
    <script src="docs/assets/js/application.js"></script>
    
    <script>
        function remove_item(arg) {
                $.ajax({url: 'http://<?=$server_ip?>:8080/remove',
                    method: 'POST',
                    data: {'guid': arg}})
                    location.replace('http://<?=$server_ip?>');
              }
    </script>

    <script>
      videojs.options.flash.swf = "dist/js/vendors/video-js.swf"
    </script>
  </body>
</html>