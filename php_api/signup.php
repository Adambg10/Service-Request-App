<?php
require_once 'db_connect.php';
header('Content-Type: application/json');
// Get JSON input
$data = json_decode(file_get_contents("php://input"));
if (isset($data->username) && isset($data->password) && isset($data->phone) && isset($data->user_type)) {
    $username = $data->username;
    $password = $data->password; // In a real app, hash this password!
    $phone = $data->phone;
    $user_type = $data->user_type;
    // Check if phone already exists
    $check_sql = "SELECT id FROM users WHERE phone = '$phone'";
    $check_result = $conn->query($check_sql);
    if ($check_result->num_rows > 0) {
        echo json_encode(array("status" => "error", "message" => "Phone number already registered"));
    } else {
        $sql = "INSERT INTO users (username, password, phone, user_type) VALUES ('$username', '$password', '$phone', '$user_type')";
        if ($conn->query($sql) === TRUE) {
            echo json_encode(array("status" => "success", "message" => "User registered successfully"));
        } else {
            echo json_encode(array("status" => "error", "message" => "Error: " . $conn->error));
        }
    }
} else {
    echo json_encode(array("status" => "error", "message" => "Missing required fields"));
}
$conn->close();
?>