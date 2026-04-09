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

if (!isset($input['intervention_id']) || !isset($input['client_id']) || !isset($input['rating'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required fields: intervention_id, client_id, rating"
    ]);
    exit;
}

$intervention_id = intval($input['intervention_id']);
$client_id = intval($input['client_id']);
$rating = intval($input['rating']);
$review = isset($input['review']) ? trim($input['review']) : '';

// Validate rating (1-5)
if ($rating < 1 || $rating > 5) {
    echo json_encode([
        "status" => "error",
        "message" => "Rating must be between 1 and 5"
    ]);
    exit;
}

try {
    // Verify the intervention belongs to this client and is completed
    $checkStmt = $pdo->prepare("
        SELECT i.id, i.provider_id 
        FROM interventions i 
        WHERE i.id = ? AND i.client_id = ? AND i.status IN ('terminee', 'echouee')
    ");
    $checkStmt->execute([$intervention_id, $client_id]);
    $intervention = $checkStmt->fetch(PDO::FETCH_ASSOC);

    if (!$intervention) {
        echo json_encode([
            "status" => "error",
            "message" => "Intervention not found or not completed"
        ]);
        exit;
    }

    $provider_id = $intervention['provider_id'];

    // Check if review already exists for this intervention
    $existsStmt = $pdo->prepare("SELECT id FROM reviews WHERE intervention_id = ?");
    $existsStmt->execute([$intervention_id]);
    
    if ($existsStmt->fetch()) {
        echo json_encode([
            "status" => "error",
            "message" => "Review already submitted for this intervention"
        ]);
        exit;
    }

    // Insert the review
    $insertStmt = $pdo->prepare("
        INSERT INTO reviews (intervention_id, client_id, provider_id, rating, review, created_at)
        VALUES (?, ?, ?, ?, ?, NOW())
    ");
    $insertStmt->execute([$intervention_id, $client_id, $provider_id, $rating, $review]);

    // Update the provider's average rating in service_providers table
    $updateRatingStmt = $pdo->prepare("
        UPDATE service_providers sp
        SET 
            rating = (SELECT AVG(r.rating) FROM reviews r WHERE r.provider_id = sp.id),
            review_count = (SELECT COUNT(*) FROM reviews r WHERE r.provider_id = sp.id)
        WHERE sp.id = ?
    ");
    $updateRatingStmt->execute([$provider_id]);

    echo json_encode([
        "status" => "success",
        "message" => "Review submitted successfully"
    ]);

} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database error: " . $e->getMessage()
    ]);
}
?>
