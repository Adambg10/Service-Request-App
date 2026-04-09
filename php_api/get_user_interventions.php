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
    $client_id = $_POST['client_id'];

    if (empty($client_id)) {
        echo json_encode(array("status" => "failure", "message" => "Client ID is required"));
        exit();
    }

    // Join with service_providers and users to get provider details
    $sql = "SELECT 
                i.id, 
                i.scheduled_time, 
                i.problem_description, 
                i.status, 
                i.price, 
                sp.profession, 
                u.username as provider_name, 
                u.phone as provider_phone
            FROM interventions i
            JOIN service_providers sp ON i.provider_id = sp.id
            JOIN users u ON sp.user_id = u.id
            WHERE i.client_id = ?
            ORDER BY i.scheduled_time DESC";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $client_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $interventions = array();

    while ($row = $result->fetch_assoc()) {
        $interventions[] = $row;
    }

    echo json_encode(array("status" => "success", "interventions" => $interventions));

    $stmt->close();
} else {
    echo json_encode(array("status" => "error", "message" => "Invalid request method"));
}

$conn->close();
?>