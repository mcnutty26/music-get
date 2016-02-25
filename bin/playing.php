<table>
            <tr>
              <td></td>
              <td></td>
              <td></td>
            </tr>
<?php
            $json = file_get_contents("http://localhost:8080/list");
            $data = json_decode($json, true);
            $json_last = file_get_contents("http://localhost:8080/last");
            $data_last = json_decode($json_last, true);
            
            $queue = array();
            $queue_temp = array();
            $last = array();

            foreach ($data as $item) {
		array_push($queue, array($item['name'], $item['ip'], $item['guid'], $item['alias']));
            }
            foreach ($data_last as $item) {
                array_push($last, $item['ip']);
            }

            $first_run = true;
            while (count($queue) > 0) {
                $ips = array();
                $bucket = array();
                foreach ($queue as $item) {
                    if ($first_run == false and !in_array($item[1], $ips)) {
                        array_push($ips, $item[1]);
                        array_push($bucket, $item);
                    } elseif ($first_run == true and !in_array($item[1], $last) and !in_array($item[1], $ips)) {
                        array_push($bucket, $item);
                        array_push($ips, $item[1]);
                    } else {
                        array_push($queue_temp, $item);
                    }
                }
                
                foreach ($bucket as $item) {
                    echo "<tr>";
                    echo "<td>" . htmlspecialchars(substr($item[0], 0, 60)) . "</td>";
		    echo "<td>" . htmlspecialchars(($item[3] != "" ? substr($item[3], 0, 20) : $item[1])) . "</td>";
                    $guid = $item[2];
                    echo "<td>" . ($client_ip == $item[1] ? "<a class=\"fui-cross clickable\" onclick=\"remove_item('$guid')\"></a>" : "") . "</td>";
                    echo "</tr>";
                }

                if (empty($bucket) == false) {
                    echo "<tr><td>&nbsp;</td><td></td><td></td></tr>";
                }

                if ($first_run == true) {
                    $first_run = false;
                }

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
