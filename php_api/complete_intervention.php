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
if (!isset($input['intervention_id']) || !isset($input['resolution'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required fields: intervention_id and resolution"
    ]);
    exit;
}

$intervention_id = intval($input['intervention_id']);
$resolution = $input['resolution']; // 'resolved' or 'failed'
$notes = isset($input['notes']) ? trim($input['notes']) : null;

// Validate resolution value
if (!in_array($resolution, ['resolved', 'failed'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid resolution value. Must be 'resolved' or 'failed'"
    ]);
    exit;
}

try {
    // Check if intervention exists and is in 'arrive' status
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

    if ($intervention['status'] !== 'arrive') {
        echo json_encode([
            "status" => "error",
            "message" => "Provider has not arrived yet"
        ]);
        exit;
    }

    // Determine new status based on resolution
    $newStatus = ($resolution === 'resolved') ? 'terminee' : 'echouee';

    // Update status and add completion time
    $stmt = $pdo->prepare("
        UPDATE interventions 
        SET status = ?, 
            completed_at = NOW(),
            resolution_notes = ?
        WHERE id = ?
    ");
    $result = $stmt->execute([$newStatus, $notes, $intervention_id]);

    if ($result) {
        echo json_encode([
            "status" => "success",
            "message" => ($resolution === 'resolved') 
                ? "Intervention marked as completed successfully" 
                : "Intervention marked as failed",
            "new_status" => $newStatus
        ]);
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Failed to update intervention"
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
?>
