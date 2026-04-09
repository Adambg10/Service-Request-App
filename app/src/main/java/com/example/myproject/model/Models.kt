package com.example.myproject.model

data class LoginRequest(
    val phone: String,
    val password: String
)

data class LoginResponse(
    val status: String,
    val message: String,
    val user: User?
)

data class SignupRequest(
    val username: String,
    val password: String,
    val phone: String,
    val user_type: String
)

data class SignupResponse(
    val status: String,
    val message: String
)

data class User(
    val id: String,
    val username: String,
    val phone: String,
    val user_type: String
)

data class ServiceProvider(
    val id: String,
    val username: String,
    val phone: String,
    val profession: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 4.5, // Default/Placeholder
    val reviewCount: Int = 0 // Default/Placeholder
)

data class ServiceProvidersResponse(
    val status: String,
    val providers: List<ServiceProvider>
)

data class InterventionRequest(
    val client_id: String,
    val provider_id: String,
    val scheduled_time: String,
    val problem_description: String,
    val latitude: Double,
    val longitude: Double
)

data class InterventionResponse(
    val status: String,
    val message: String
)

data class Intervention(
    val id: String,
    val scheduled_time: String,
    val problem_description: String,
    val status: String,
    val price: String?,
    val profession: String,
    val provider_name: String,
    val provider_phone: String
)

data class UserInterventionsResponse(
    val status: String,
    val interventions: List<Intervention>
)

data class ProviderIntervention(
    val id: String,
    val client_id: String,
    val scheduled_time: String,
    val problem_description: String,
    val status: String,
    val created_at: String,
    val client_name: String,
    val client_phone: String?,
    val client_latitude: Double?,
    val client_longitude: Double?,
    var distance_km: Float? = null // Calculated on device
)

data class ProviderInterventionsResponse(
    val status: String,
    val interventions: List<ProviderIntervention>
)

data class UpdateStatusRequest(
    val intervention_id: String,
    val status: String
)

data class UpdateStatusResponse(
    val status: String,
    val message: String
)

data class LocationUpdateRequest(
    val user_id: String,
    val latitude: Double,
    val longitude: Double
)

data class LocationUpdateResponse(
    val status: String,
    val message: String
)

data class ActiveIntervention(
    val intervention_id: String,
    val status: String,
    val problem_description: String,
    val scheduled_time: String,
    val provider_id: String,
    val provider_name: String,
    val provider_phone: String,
    val provider_profession: String,
    val provider_latitude: Double?,
    val provider_longitude: Double?
)

data class ActiveInterventionResponse(
    val status: String,
    val has_active: Boolean,
    val intervention: ActiveIntervention?
)

data class MarkArrivedRequest(
    val intervention_id: String
)

data class CompleteInterventionRequest(
    val intervention_id: String,
    val resolution: String, // "resolved" or "failed"
    val notes: String? = null
)

data class GenericResponse(
    val status: String,
    val message: String
)

data class SubmitReviewRequest(
    val intervention_id: Int,
    val client_id: Int,
    val rating: Int,
    val review: String
)
