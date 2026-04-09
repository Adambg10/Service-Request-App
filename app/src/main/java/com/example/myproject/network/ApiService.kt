package com.example.myproject.network

import com.example.myproject.model.LoginRequest
import com.example.myproject.model.LoginResponse
import com.example.myproject.model.SignupRequest
import com.example.myproject.model.SignupResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login.php")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("signup.php")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    @retrofit2.http.FormUrlEncoded
    @POST("get_user_details.php")
    fun getUserDetails(@retrofit2.http.Field("user_id") userId: String): Call<LoginResponse>

    @retrofit2.http.GET("get_service_providers.php")
    fun getServiceProviders(): Call<com.example.myproject.model.ServiceProvidersResponse>

    @POST("create_intervention.php")
    fun createIntervention(@Body request: com.example.myproject.model.InterventionRequest): Call<com.example.myproject.model.InterventionResponse>

    @retrofit2.http.FormUrlEncoded
    @POST("get_user_interventions.php")
    fun getUserInterventions(@retrofit2.http.Field("client_id") clientId: String): Call<com.example.myproject.model.UserInterventionsResponse>

    @retrofit2.http.GET("get_provider_interventions.php")
    fun getProviderInterventions(@retrofit2.http.Query("provider_id") providerId: String): Call<com.example.myproject.model.ProviderInterventionsResponse>

    @POST("update_intervention_status.php")
    fun updateInterventionStatus(@Body request: com.example.myproject.model.UpdateStatusRequest): Call<com.example.myproject.model.UpdateStatusResponse>

    @POST("update_provider_location.php")
    fun updateProviderLocation(@Body request: com.example.myproject.model.LocationUpdateRequest): Call<com.example.myproject.model.LocationUpdateResponse>

    @POST("update_client_location.php")
    fun updateClientLocation(@Body request: com.example.myproject.model.LocationUpdateRequest): Call<com.example.myproject.model.LocationUpdateResponse>

    @retrofit2.http.GET("get_active_intervention.php")
    fun getActiveIntervention(@retrofit2.http.Query("client_id") clientId: String): Call<com.example.myproject.model.ActiveInterventionResponse>

    @POST("mark_provider_arrived.php")
    fun markProviderArrived(@Body request: com.example.myproject.model.MarkArrivedRequest): Call<com.example.myproject.model.GenericResponse>

    @POST("complete_intervention.php")
    fun completeIntervention(@Body request: com.example.myproject.model.CompleteInterventionRequest): Call<com.example.myproject.model.GenericResponse>

    @POST("submit_review.php")
    fun submitReview(@Body request: com.example.myproject.model.SubmitReviewRequest): Call<com.example.myproject.model.GenericResponse>
}
