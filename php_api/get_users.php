<?php
$servername = "localhost";
$username = "root";
$password = ""; // Default XAMPP password is empty
$dbname = "home_services_db";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
  die("Connection failed: " . $conn->connect_error);
}

// Get data
$sql = "SELECT id, username FROM users";
$result = $conn->query($sql);

$response = array();

if ($result->num_rows > 0) {
  while($row = $result->fetch_assoc()) {
    // Push each row into the array
    array_push($response, $row);
  }
}

// Return JSON
echo json_encode($response);
$conn->close();
?>