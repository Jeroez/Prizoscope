import com.example.prizoscope.api.ObjectDetectionResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ObjectDetectionApiService {
    @Headers("Content-Type: application/json") // Set content type for JSON request
    @POST("images:annotate") // Keep the endpoint clean, API key will be added elsewhere
    fun detectObjects(
        @Body requestBody: MultipartBody.Part // JSON request body
    ): Call<ObjectDetectionResponse>
}
