import javafx.util.Pair;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Created by KevinRoj on 26.04.17.
 */
@TuneableParameters()
public class AutoTuneDefault<T extends Serializable> extends AutoTune<T> {
    @NotNull private List<Pair<List<Double>, Double>>sampledConfigurations = new ArrayList<>(10);
    @NotNull private List<List<Double>>cachedConfiguration  = new ArrayList<>(10);
    @Nullable private List<Double> currentConfiguration;

    private int cacheSize;

    //MOE hyper-parameter
    @NumericParameter(min=0.2, max=30)
    int lengthScaleDivider = 5;
    @NumericParameter(min=1, max=30)
    int numOptimizerMultistarts =  3;
    @NumericParameter(min=0.0001, max=2.0)
    double gaussianSignalVariance = 1.0;

    private boolean useDefaultValues = true;

    private boolean initRandomSearch = false;

    private OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(320, TimeUnit.SECONDS)
            .readTimeout(320, TimeUnit.SECONDS)
            .writeTimeout(320, TimeUnit.SECONDS)
            .build();

    private double bestResult = Double.MAX_VALUE;
    @Nullable private List<Double> bestConfiguration;
    @Nullable private T currentConfigurationObject;
    @Nullable private T bestConfigurationObject;

    public AutoTuneDefault(T config){
        super(config);
    }

    @Override
    public T getConfig() throws IllegalAccessException {
        //extract class information
        TuneableParameters tuneSettings = config.getClass().getAnnotation(TuneableParameters.class);
        cacheSize = tuneSettings.cacheNextPoints();

        /** extract numeric field information **/
        final List<Field> numericFields = FieldUtils.getFieldsListWithAnnotation(config.getClass(), NumericParameter.class);

        //create domain info
        GpNextPointsRequest req = new GpNextPointsRequest(
                cacheSize,
                new GpNextPointsRequest.OptimizerInfo(numOptimizerMultistarts, "gradient_descent_optimizer"),
                new GpNextPointsRequest.CovarianceInfo(),
                new GpNextPointsRequest.BoundedDomainInfo(),
                new GpNextPointsRequest.GpHistoricalInfo());

        req.getCovariance_info().getHyperparameters().add(gaussianSignalVariance); //add signal variance information, for the gaussian process
        for (Field field : numericFields){
            NumericParameter numericParameterInfo = field.getAnnotation(NumericParameter.class);
            GpNextPointsRequest.Domain boundA = new GpNextPointsRequest.Domain(numericParameterInfo.max(), numericParameterInfo.min());
            req.getDomain_info().getDomain_bounds().add(boundA);

            //length scale determines how closely two sample points are correlated
            final double lengthScale = (numericParameterInfo.max() - numericParameterInfo.min()) / lengthScaleDivider;
            req.getCovariance_info().getHyperparameters().add(lengthScale);
        }

        /** extract nominal field information **/
        final List<Field> nominalFields = FieldUtils.getFieldsListWithAnnotation(config.getClass(), NominalParameter.class);

        req.getCovariance_info().getHyperparameters().add(gaussianSignalVariance); //add signal variance information, for the gaussian process
        for (Field field : nominalFields){
            NominalParameter nominalParameterInfo = field.getAnnotation(NominalParameter.class);

            GpNextPointsRequest.Domain boundA = new GpNextPointsRequest.Domain(nominalParameterInfo.values().length, 0);
            req.getDomain_info().getDomain_bounds().add(boundA);

            //length scale for nominal value is one
            req.getCovariance_info().getHyperparameters().add(1.0);
        }

        final int dimension = numericFields.size() + nominalFields.size();

        req.getDomain_info().updateDimension();


        //random search at the begin
        if(!initRandomSearch){
            final int samplesPerDimension = (int) Math.ceil(Math.log10(tuneSettings.initRandomSearch()) / Math.log10(dimension));

            final int numberOfSamples = (int) Math.pow(samplesPerDimension, dimension);
            cachedConfiguration = new ArrayList<>(numberOfSamples);

            //prepare cachedConfiguration
            for (int i = 0; i < numberOfSamples; i++) {
                cachedConfiguration.add(new ArrayList<>(numericFields.size()));
            }

            //fill cachedConfiguration with latin hypercube samples by dimension
            for (int i = 0; i < numericFields.size(); i++) { //for numericFields
                NumericParameter numericParameterInfo = numericFields.get(i).getAnnotation(NumericParameter.class);
                final double parameterWidth = (numericParameterInfo.max() - numericParameterInfo.min()) / samplesPerDimension;
                Random rand = new Random();
                for(int j = 0; j < numberOfSamples; j++){ //TODO latin hypercube ausbessern
                    cachedConfiguration.get(j).add(rand.nextDouble()*parameterWidth + numericParameterInfo.min() + ((Math.floor((j/(i+1)))) % dimension) * parameterWidth);
                }
            }
            for (int i = 0; i < nominalFields.size(); i++) { //for nominalFields
                NominalParameter nominalParameterInfo = nominalFields.get(i).getAnnotation(NominalParameter.class);
                final double parameterWidth = nominalParameterInfo.values().length/samplesPerDimension;
                Random rand = new Random();
                for(int j = 0; j < numberOfSamples; j++){
                    cachedConfiguration.get(j).add(rand.nextDouble()*parameterWidth  + ((Math.floor((j/(i+1)))) % dimension) * parameterWidth);
                }
            }

            initRandomSearch = true;

            if(useDefaultValues) {
                //add default values for probing (numeric values)
                ArrayList<Double> predefinedDefaultParameters = new ArrayList<>(numericFields.size()+nominalFields.size());
                for (Field field : numericFields) {
                    predefinedDefaultParameters.add(field.getDouble(config));
                }
                for (Field field : nominalFields) {
                    NominalParameter nominalParameterInfo = field.getAnnotation(NominalParameter.class);
                    String strLabel;
                    if(field.getType().equals(boolean.class)){
                        strLabel = String.valueOf(field.getBoolean(config));
                    }else {
                        strLabel = (String) field.get(config);
                    }
                    int i;
                    for (i = 0; i < nominalParameterInfo.values().length; i++) {
                        if(nominalParameterInfo.values()[i].equals(strLabel)){
                            break;
                        }
                    }
                    predefinedDefaultParameters.add(new Double(i));
                }
                cachedConfiguration.add(predefinedDefaultParameters);
            }

        }else if(cachedConfiguration.size() == 0){
            //fill cache from REST optimizer
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://127.0.0.1:6543")
                    .client(okHttpClient)
                    .addConverterFactory(JacksonConverterFactory.create())
                    .build();

            MOE service = retrofit.create(MOE.class);

            for(Pair<List<Double>, Double> sample : sampledConfigurations){

                req.getGp_historical_info().getPoints_sampled().add(new GpNextPointsRequest.SinglePoint(sample.getKey(), sample.getValue(), 0.1));
            }


            Call<GpNextPointsResponse> nextPointsCall = service.gpNextPointsEpi(req);
            try {
                Response<GpNextPointsResponse> response = nextPointsCall.execute();
                GpNextPointsResponse nextPointsResponse = response.body();
                cachedConfiguration = nextPointsResponse.getPoints_to_sample();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //use cached configuration
        currentConfiguration = cachedConfiguration.get(cachedConfiguration.size()-1);
        cachedConfiguration.remove(cachedConfiguration.size()-1);



        Iterator<Double> currentConfigurationItr = currentConfiguration.iterator();
        for (Field field : numericFields){ //for numeric fields
            if(field.getType().equals(long.class)){
                field.setLong(config, currentConfigurationItr.next().longValue());
            }else if(field.getType().equals(int.class)){
                field.setInt(config, currentConfigurationItr.next().intValue());
            }else {
                //assume it is a double parameter
                field.setDouble(config, currentConfigurationItr.next());
            }
        }
        for (Field field : nominalFields){ //for nominal fields
            NominalParameter nominalParameterInfo = field.getAnnotation(NominalParameter.class);
            int label = currentConfigurationItr.next().intValue();
            label = label < 0 ? 0 : label;
            label = label >=  nominalParameterInfo.values().length ? nominalParameterInfo.values().length-1 : label;

            String strLabel = nominalParameterInfo.values()[label];

            if(field.getType().equals(boolean.class)){
                field.setBoolean(config, Boolean.parseBoolean(strLabel));
            }else {
                //assume it is a String parameter
                field.set(config, strLabel);
            }
        }
        this.currentConfigurationObject = config;
        return config;
    }

    @Override
    public void setResult(double amount) {
        if(!Double.isFinite(amount)){
            //sanitize result
            amount = Double.MAX_VALUE;
        }
        if(amount < this.bestResult){
            //new best result
            this.bestResult = amount;
            bestConfiguration = currentConfiguration;
            bestConfigurationObject = currentConfigurationObject;
        }
        sampledConfigurations.add(new Pair<>(currentConfiguration, amount));
    }

    @Override
    public T getBestConfiguration() {
        return this.bestConfigurationObject;
    }

    @Override
    List<Double> getBestConfigurationParameter() {
        return null;
    }

    @Override
    public double getBestResult() {
        return this.bestResult;
    }

}
