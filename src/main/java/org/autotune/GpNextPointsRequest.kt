package org.autotune

/**
 * Created by KevinRoj on 26.04.17.
 */
data class GpNextPointsRequest(val num_to_sample: Int = 1, val optimizer_info: org.autotune.GpNextPointsRequest.OptimizerInfo = OptimizerInfo(), val covariance_info: org.autotune.GpNextPointsRequest.CovarianceInfo = CovarianceInfo(), val domain_info: org.autotune.GpNextPointsRequest.BoundedDomainInfo = org.autotune.GpNextPointsRequest.BoundedDomainInfo(), val gp_historical_info: org.autotune.GpNextPointsRequest.GpHistoricalInfo = GpHistoricalInfo()) {

    data class BoundedDomainInfo(var dim: Int = 1, val domain_bounds: List<org.autotune.GpNextPointsRequest.Domain> = kotlin.collections.ArrayList<Domain>()) {
        fun updateDimension(){
            this.dim = this.domain_bounds.size
        }
    }

    data class Domain(val max: Double, val min: Double)

    data class GpHistoricalInfo(var points_sampled: List<org.autotune.GpNextPointsRequest.SinglePoint> = kotlin.collections.ArrayList<SinglePoint>())

    data class SinglePoint(val point: List<Double> = kotlin.collections.ArrayList<Double>(), val value: Double, val value_var: Double)

    data class OptimizerInfo(val num_multistarts: Int = 3, val optimizer_type: String = "gradient_descent_optimizer")

    //signal variance, length scale #1, length scale #2
    data class CovarianceInfo(var hyperparameters: List<Double> = kotlin.collections.ArrayList<Double>(), val covariance_type: String = "square_exponential")
}

