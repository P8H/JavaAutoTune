import kotlin.collections.ArrayList

/**
 * Created by KevinRoj on 26.04.17.
 */
data class GpNextPointsRequest(val num_to_sample: Int = 1, val optimizer_info: OptimizerInfo = OptimizerInfo(), val covariance_info: CovarianceInfo = CovarianceInfo(),val domain_info: BoundedDomainInfo = GpNextPointsRequest.BoundedDomainInfo(), val gp_historical_info: GpHistoricalInfo =  GpHistoricalInfo()){

    data class BoundedDomainInfo(var dim: Int = 1, val domain_bounds: List<Domain> = ArrayList<Domain>()){
        fun updateDimension(){
            this.dim = this.domain_bounds.size
        }
    }

    data class Domain(val max: Double, val min: Double)

    data class GpHistoricalInfo(var points_sampled: List<SinglePoint> = ArrayList<SinglePoint>())

    data class SinglePoint(val point: List<Double> = ArrayList<Double>(), val value: Double, val value_var: Double)

    data class OptimizerInfo(val num_multistarts: Int = 3, val optimizer_type: String = "gradient_descent_optimizer")

    //signal variance, length scale #1, length scale #2
    data class CovarianceInfo(var hyperparameters: List<Double> = ArrayList<Double>(), val covariance_type: String = "square_exponential")
}

