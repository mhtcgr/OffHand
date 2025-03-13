// 在 AnalysisResponse.kt 中修改所有数据类
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ApiResponse(
    val code: String,
    val message: String,
    val data: AnalysisData
) : Parcelable

@Parcelize
data class AnalysisData(
    val shootingAngles: ShootingAngles,
    val analysis: String,
    val suggestions: String,
    val weaknessPoints: String
) : Parcelable

@Parcelize
data class ShootingAngles(
    val userId: String,
    val recordId: String,
    val aimingElbowAngle: Double,
    val aimingArmAngle: Double,
    val aimingBodyAngle: Double,
    val aimingKneeAngle: Double,
    val releaseElbowAngle: Double,
    val releaseArmAngle: Double,
    val releaseBodyAngle: Double,
    val releaseKneeAngle: Double,
    val releaseWristAngle: Double,
    val releaseBallAngle: Double,
    val theme: String
) : Parcelable