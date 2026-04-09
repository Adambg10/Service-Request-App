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

// Get POST data (JSON body)
$input = json_decode(file_get_contents("php://input"), true);

// Validate required fields
if (!isset($input['user_id']) || !isset($input['latitude']) || !isset($input['longitude'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required fields: user_id, latitude, longitude"
    ]);
    exit;
}

$user_id = intval($input['user_id']);
$latitude = floatval($input['latitude']);
$longitude = floatval($input['longitude']);

// Validate latitude and longitude ranges
if ($latitude < -90 || $latitude > 90) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid latitude value. Must be between -90 and 90."
    ]);
    exit;
}

if ($longitude < -180 || $longitude > 180) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid longitude value. Must be between -180 and 180."
    ]);
    exit;
}

try {
    // Check if user exists and is a client
    $checkStmt = $pdo->prepare("SELECT id, user_type FROM users WHERE id = ?");
    $checkStmt->execute([$user_id]);
    $user = $checkStmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) {
        echo json_encode([
            "status" => "error",
            "message" => "User not found"
        ]);
        exit;
    }

    // Update user's location
    $stmt = $pdo->prepare("UPDATE users SET latitude = ?, longitude = ? WHERE id = ?");
    $result = $stmt->execute([$latitude, $longitude, $user_id]);

    if ($result) {
        echo json_encode([
            "status" => "success",
            "message" => "Location updated successfully",
            "data" => [
                "user_id" => $user_id,
                "latitude" => $latitude,
                "longitude" => $longitude
            ]
        ]);
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Failed to update location"
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
?>