package org.autotune;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by KevinRoj on 26.04.17.
 */
public interface MOE {
    @POST("gp/next_points/epi")
    Call<GpNextPointsResponse> gpNextPointsEpi(@Body GpNextPointsRequest request);

    @POST("gp/next_points/constant_liar")
    Call<GpNextPointsResponse> gpNextPointsConstantLiar(@Body GpNextPointsRequest request);

    @POST("gp/next_points/kriging")
    Call<GpNextPointsResponse> gpNextPointsKriging(@Body GpNextPointsRequest request);


}
