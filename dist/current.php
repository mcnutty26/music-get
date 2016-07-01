<?php
	$json = file_get_contents("http://localhost:8080/current");
	$data = json_decode($json, true);

	$item = $data[0];
	$name = $item['name'];
	$ip = $item['ip'];
	$guid = $item['guid'];
	$alias = $item['alias'];
	$display = ($alias == "" ? $ip : $alias);

	if ($guid != ""){ 
		echo "<a href=\"/files/$guid\">$name by $display</a>";
	} else {
		echo "Nothing playing. Please queue more memes.";
	}
?>
