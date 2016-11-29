<?php
	$client_ip = $_COOKIE['music-get-client']; 
	$json = file_get_contents("http://localhost/api/downloading");
	$data = json_decode($json, true);
	foreach ($data as $item) {
		if ($client_ip == $item['ip']) {
			echo "Downloading " . $item['name'] . "<br>";
		}
	}
?>
