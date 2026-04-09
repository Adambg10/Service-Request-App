<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET");
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

// Get client_id from query parameter
if (!isset($_GET['client_id'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required parameter: client_id"
    ]);
    exit;
}

$client_id = intval($_GET['client_id']);

try {
    // Query to get the active (accepted) intervention for this client
    // Join with service_providers to get provider details and their live location
    $stmt = $pdo->prepare("
        SELECT 
            i.id AS intervention_id,
            i.status,
            i.problem_description,
            i.scheduled_time,
            sp.id AS provider_id,
            u.username AS provider_name,
            u.phone AS provider_phone,
            sp.profession AS provider_profession,
            u.latitude AS provider_latitude,
            u.longitude AS provider_longitude
        FROM interventions i
        JOIN service_providers sp ON i.provider_id = sp.id
        JOIN users u ON sp.user_id = u.id
        WHERE i.client_id = ? AND i.status IN ('acceptee', 'arrive')
        ORDER BY i.created_at DESC
        LIMIT 1
    ");
    $stmt->execute([$client_id]);
    $intervention = $stmt->fetch(PDO::FETCH_ASSOC);

    if ($intervention) {
        // Convert numeric strings to proper types
        $intervention['provider_latitude'] = $intervention['provider_latitude'] !== null ? floatval($intervention['provider_latitude']) : null;
        $intervention['provider_longitude'] = $intervention['provider_longitude'] !== null ? floatval($intervention['provider_longitude']) : null;

        echo json_encode([
            "status" => "success",
            "has_active" => true,
            "intervention" => $intervention
        ]);
    } else {
        echo json_encode([
            "status" => "success",
            "has_active" => false,
            "intervention" => null
        ]);
    }

} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
?>
