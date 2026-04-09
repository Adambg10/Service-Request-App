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
    $intervention_id = isset($data['intervention_id']) ? $data['intervention_id'] : null;
    $status = isset($data['status']) ? $data['status'] : null;
    if (empty($intervention_id) || empty($status)) {
        echo json_encode(array("status" => "failure", "message" => "Intervention ID and status are required"));
        exit();
    }
    // Validate status
    $allowed_statuses = ['en_attente', 'acceptee', 'terminee', 'annulee'];
    if (!in_array($status, $allowed_statuses)) {
        echo json_encode(array("status" => "failure", "message" => "Invalid status"));
        exit();
    }
    $stmt = $conn->prepare("UPDATE interventions SET status = ? WHERE id = ?");
    $stmt->bind_param("si", $status, $intervention_id);
    if ($stmt->execute()) {
        echo json_encode(array("status" => "success", "message" => "Intervention status updated successfully"));
    } else {
        echo json_encode(array("status" => "failure", "message" => "Error: " . $stmt->error));
    }
    $stmt->close();
} else {
    echo json_encode(array("status" => "error", "message" => "Invalid request method"));
}
$conn->close();
?>