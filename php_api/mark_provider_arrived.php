<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

// Database configuration
$host = "localhost";
$dbname = "home_services_db";
$username = "root";
$password = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database connection failed: " . $e->getMessage()
    ]);
    exit;
}

// Get JSON input
$input = json_decode(file_get_contents("php://input"), true);

// Validate required fields
if (!isset($input['intervention_id'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required field: intervention_id"
    ]);
    exit;
}

$intervention_id = intval($input['intervention_id']);

try {
    // Check if intervention exists and is in 'acceptee' status
    $checkStmt = $pdo->prepare("SELECT id, status FROM interventions WHERE id = ?");
    $checkStmt->execute([$intervention_id]);
    $intervention = $checkStmt->fetch(PDO::FETCH_ASSOC);

    if (!$intervention) {
        echo json_encode([
            "status" => "error",
            "message" => "Intervention not found"
        ]);
        exit;
    }

    if ($intervention['status'] !== 'acceptee') {
        echo json_encode([
            "status" => "error",
            "message" => "Intervention is not in accepted status"
        ]);
        exit;
    }

    // Update status to 'arrive'
    $stmt = $pdo->prepare("UPDATE interventions SET status = 'arrive' WHERE id = ?");
    $result = $stmt->execute([$intervention_id]);

    if ($result) {
        echo json_encode([
            "status" => "success",
            "message" => "Provider marked as arrived"
        ]);
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Failed to update status"
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
?>
