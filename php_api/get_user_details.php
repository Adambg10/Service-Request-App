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
    $user_id = $_POST['user_id'];
    if (empty($user_id)) {
        echo json_encode(array("status" => "failure", "message" => "User ID is required"));
        exit();
    }
    $stmt = $conn->prepare("SELECT id, username, phone, user_type FROM users WHERE id = ?");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        echo json_encode(array("status" => "success", "user" => $row));
    } else {
        echo json_encode(array("status" => "failure", "message" => "User not found"));
    }
    $stmt->close();
} else {
    echo json_encode(array("status" => "error", "message" => "Invalid request method"));
}
$conn->close();
?>