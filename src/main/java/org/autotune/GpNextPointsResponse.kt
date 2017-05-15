package org.autotune

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Created by KevinRoj on 26.04.17.
 */
data class GpNextPointsResponse(val endpoint: String = "", val points_to_sample: List<List<Double>> = ArrayList<List<Double>>(), val status: GpNextPointsStatus = GpNextPointsStatus()){
    data class GpNextPointsStatus(val expected_improvement: Double = 0.0, @JsonIgnore val optimizer_success: String = "")
}