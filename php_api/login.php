<?php
require_once 'db_connect.php';
header('Content-Type: application/json');
// Get JSON input
$data = json_decode(file_get_contents("php://input"));
if (isset($data->phone) && isset($data->password)) {
    $phone = $data->phone;
    $password = $data->password;
    $sql = "SELECT id, username, user_type FROM users WHERE phone = '$phone' AND password = '$password'";
    $result = $conn->query($sql);
    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        echo json_encode(array(
            "status" => "success",
            "message" => "Login successful",
            "user" => $row
        ));
    } else {
        echo json_encode(array("status" => "error", "message" => "Invalid phone or password"));
    }
} else {
    echo json_encode(array("status" => "error", "message" => "Missing phone or password"));
}
$conn->close();
?>