<?php
    $server_ip = $_SERVER['SERVER_ADDR'];
    $pw = trim(file_get_contents("config.ini"));
    session_start();
    if ($_POST['login'] == 2) {
        session_destroy();
        header( 'Location: admin.php' ) ;
    }
    if ($_POST['pw'] == $pw) {
        $_SESSION['login'] = $pw;
    }
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
        <h3>Admin Panel</h3>
      </div>

      <div class="login-form">
        <?php if ($_SESSION['login'] == $pw) { ?>
        <div class="row">
          <div class="col-xs-12">
		  <table>
            <tr>
              <td></td>
              <td></td>
              <td></td>
            </tr>
        <?php
            $json = file_get_contents("http://localhost:8080/list");
            $data = json_decode($json, true);
            $queue = array();
            $queue_temp = array();
            foreach ($data as $item) {
                array_push($queue, array($item['name'], $item['ip'], $item['guid']));
            }
            while (count($queue) > 0) {
                $ips = array();
                $bucket = array();
                foreach ($queue as $item) {
                    if (!in_array($item[1], $ips)) {
                        array_push($ips, $item[1]);
                        array_push($bucket, $item);
                    } else {
                        array_push($queue_temp, $item);
                    }
                }
                foreach ($bucket as $item) {
                    echo "<tr>";
                    echo "<td>" . $item[0] . "</td>";
                    echo "<td>" . $item[1] . "</td>";
                    $guid = $item[2];
                    echo "<td><a class=\"fui-cross ajax-button\" onclick=\"remove_item('$guid')\"></a></td>";
                    echo "</tr>";
                }
                echo "<tr><td>&nbsp;</td><td></td><td></td></tr>";
                unset($queue);
                $queue = array();
                unset($bucket);
                $bucket = array();
                $queue = $queue_temp;
                unset($queue_temp);
                $queue_temp = array();
            }
             ?>
          </table>
          </div>
		</div>
        <div class="row">
          <div class="col-xs-6">
            <input type="submit" class="btn btn-danger btn-lg btn-block" onclick="remove_current()" value="Kill current item">
          </div>
          <div class="col-xs-6">
		    <form method="post" action="admin.php">
              <input type="hidden" name="login" value="2">
              <input type="submit" class="btn btn-primary btn-lg btn-block" value="Log out">
            </form>
          </div>
		</div>
        <?php } else { ?>
        <form method="post" action="admin.php">
          <div class="form-group">
            <input type="password" class="form-control login-field" value="" placeholder="Password" id="pw" name="pw" required />
          </div>
            <input class="btn btn-primary btn-lg btn-block" type="submit" value="Login">
        </form>
        <?php } ?>
      </div>

    </div> <!-- /container -->

    <script src="dist/js/vendor/jquery.min.js"></script>
    <script src="dist/js/vendor/video.js"></script>
    <script src="dist/js/flat-ui.min.js"></script>
    <script src="docs/assets/js/application.js"></script>
    
    <script>
        function remove_item(arg) {
            $.ajax({url: 'http://<?=$server_ip?>:8080/admin/remove',
                method: 'POST',
                data: {'guid': arg, 'pw' : '<?=$pw?>'}})
                location.replace('http://<?=$server_ip?>/admin.php');
        }
        function remove_current() {
            $.ajax({url: 'http://<?=$server_ip?>:8080/admin/kill',
                method: 'POST',
                data: {'pw' : '<?=$pw?>'}})
                location.replace('http://<?=$server_ip?>/admin.php');
        }
    </script>

    <script>
      videojs.options.flash.swf = "dist/js/vendors/video-js.swf"
    </script>
  </body>
</html>
