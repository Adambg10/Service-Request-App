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
    $client_id = isset($data['client_id']) ? $data['client_id'] : null;
    $provider_id = isset($data['provider_id']) ? $data['provider_id'] : null;
    $scheduled_time = isset($data['scheduled_time']) ? $data['scheduled_time'] : null;
    $problem_description = isset($data['problem_description']) ? $data['problem_description'] : null;
    if (empty($client_id) || empty($provider_id) || empty($scheduled_time) || empty($problem_description)) {
        echo json_encode(array("status" => "failure", "message" => "All fields are required"));
        exit();
    }
    $stmt = $conn->prepare("INSERT INTO interventions (client_id, provider_id, scheduled_time, problem_description) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("iiss", $client_id, $provider_id, $scheduled_time, $problem_description);
    if ($stmt->execute()) {
        echo json_encode(array("status" => "success", "message" => "Intervention request created successfully"));
    } else {
        echo json_encode(array("status" => "failure", "message" => "Error: " . $stmt->error));
    }
    $stmt->close();
} else {
    echo json_encode(array("status" => "error", "message" => "Invalid request method"));
}
$conn->close();
?>