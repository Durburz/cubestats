<!--
   stats.php
   
   Copyright 2014 Durburz <durburz@valkyr.eu>
-->

<?php
$host = "localhost";
$user = "user";
$password = "password";
$database = "cubestats";

$servername = "ServerName";
$interval = 60;

$connection = mysql_connect($host,$user,$password) 
	or die("Could not connect: ".mysql_error());

mysql_select_db($database,$connection) 
	or die("Error in selecting the database:".mysql_error());
	
$serversessions="
SELECT UUID, start, end
FROM session
WHERE UUID = '".$servername."'
ORDER BY start ASC;";

$serversessions_result=mysql_query($serversessions,$connection) 
	or exit("Sql Error".mysql_error());

$serversessions_num=mysql_num_rows($serversessions_result);


$playersessions="
SELECT UUID, start, end
FROM session
WHERE UUID <> '".$servername."'
ORDER BY start ASC;";

$playersessions_result=mysql_query($playersessions,$connection) 
	or exit("Sql Error".mysql_error());

$playersessions_num=mysql_num_rows($playersessions_result);
?>
<?php
	$c = 0;
	$max = 0;
	$servertime = 0;
	while ($row = mysql_fetch_assoc($serversessions_result)) {
		if ($c == 0) {
			$max = $max - $row['start'];
		}
		if ($c == ($serversessions_num - 1)) {
			if ($row['end'] + $interval < time()) {
				$max = $max + time();
			}
			else {
				$max = $max + $row['end'];
			}
		}
		$servertime = $servertime + $row['end'] - $row['start'];
		echo "Uptime: ".round($servertime/$max*100,1)."%";
		$c++;
	}
?>
<?php
echo "<br/><br/>";
	$c = 0;
	$max = 0;
	$playertime = 0;
	while ($row = mysql_fetch_assoc($playersessions_result)) {
		echo $row['UUID']." ".$row['start']." ".$row['end']." ".($row['end'] - $row['start'])."<br/>";
	}
?>