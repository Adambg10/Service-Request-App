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
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    // Read JSON input
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    $user_id = isset($data['user_id']) ? $data['user_id'] : null;
    $latitude = isset($data['latitude']) ? $data['latitude'] : null;
    $longitude = isset($data['longitude']) ? $data['longitude'] : null;
    if (empty($user_id) || empty($latitude) || empty($longitude)) {
        echo json_encode(array("status" => "failure", "message" => "User ID, latitude, and longitude are required"));
        exit();
    }
    // Update location in users table where id matches
    $stmt = $conn->prepare("UPDATE users SET latitude = ?, longitude = ? WHERE id = ?");
    $stmt->bind_param("ddi", $latitude, $longitude, $user_id);
    if ($stmt->execute()) {
        if ($stmt->affected_rows > 0) {
            echo json_encode(array("status" => "success", "message" => "Location updated successfully"));
        } else {
            // Could mean user_id not found in service_providers or location didn't change
            echo json_encode(array("status" => "success", "message" => "Location updated (or no change needed)"));
        }
    } else {
        echo json_encode(array("status" => "failure", "message" => "Error: " . $stmt->error));
    }
    $stmt->close();
} else {
    echo json_encode(array("status" => "error", "message" => "Invalid request method"));
}
$conn->close();
?>