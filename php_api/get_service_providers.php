<?php
header("Content-Type: application/json; charset=UTF-8");
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "home_services_db";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die(json_encode(array("status" => "error", "message" => "Connection failed: " . $conn->connect_error)));
}

// Read latitude/longitude from service_providers, but username/phone from users
$sql = "SELECT sp.id, sp.profession, sp.description, sp.latitude, sp.longitude, u.username, u.phone 
        FROM service_providers sp 
        JOIN users u ON sp.user_id = u.id
        WHERE sp.latitude IS NOT NULL AND sp.longitude IS NOT NULL";

$result = $conn->query($sql);
$providers = array();

if ($result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        // Convert lat/lng to floats for proper JSON format
        $row['latitude'] = floatval($row['latitude']);
        $row['longitude'] = floatval($row['longitude']);
        $providers[] = $row;
    }
    echo json_encode(array("status" => "success", "providers" => $providers));
} else {
    echo json_encode(array("status" => "success", "providers" => []));
}

$conn->close();
?>