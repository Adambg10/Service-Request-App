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

// Get provider_id from query parameter
if (!isset($_GET['provider_id'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required parameter: provider_id"
    ]);
    exit;
}

$provider_id = intval($_GET['provider_id']);

try {
    // First, get the service_providers.id from the users.id
    // The app passes users.id, but interventions.provider_id references service_providers.id
    $spStmt = $pdo->prepare("SELECT id FROM service_providers WHERE user_id = ?");
    $spStmt->execute([$provider_id]);
    $serviceProvider = $spStmt->fetch(PDO::FETCH_ASSOC);

    if (!$serviceProvider) {
        echo json_encode([
            "status" => "success",
            "interventions" => []
        ]);
        exit;
    }

    $service_provider_id = $serviceProvider['id'];

    // Query to get all interventions for this provider
    // Client location comes from users table

    $stmt = $pdo->prepare("
        SELECT 
            i.id,
            i.client_id,
            i.scheduled_time,
            i.problem_description,
            i.status,
            i.created_at,
            u.username AS client_name,
            u.phone AS client_phone,
            u.latitude AS client_latitude,
            u.longitude AS client_longitude
        FROM interventions i
        JOIN users u ON i.client_id = u.id
        WHERE i.provider_id = ?
        ORDER BY 
            CASE 
                WHEN i.status = 'en_attente' THEN 1
                WHEN i.status = 'acceptee' THEN 2
                WHEN i.status = 'arrive' THEN 3
                ELSE 4
            END,
            i.created_at DESC
    ");
    $stmt->execute([$service_provider_id]);
    $interventions = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Convert numeric strings to proper types
    foreach ($interventions as &$intervention) {
        $intervention['client_latitude'] = $intervention['client_latitude'] !== null ? floatval($intervention['client_latitude']) : null;
        $intervention['client_longitude'] = $intervention['client_longitude'] !== null ? floatval($intervention['client_longitude']) : null;
    }

    echo json_encode([
        "status" => "success",
        "interventions" => $interventions
    ]);

} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
?>