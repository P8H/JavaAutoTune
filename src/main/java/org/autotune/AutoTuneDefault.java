package org.autotune;

import javafx.util.Pair;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.stream.Collectors;


/**
 * Created by KevinRoj on 26.04.17.
 */
@TuneableParameters()
public class AutoTuneDefault<T extends Serializable> extends AutoTune<T> {
    @NotNull
    final static protected Logger logger = LogManager.getLogger(AutoTuneDefault.class);

    @NotNull private List<Pair<List<Double>, Double>>sampledConfigurations = new ArrayList<>(10);
    @NotNull private List<List<Double>>cachedConfiguration  = new ArrayList<>(10);
    @Nullable private List<Double> currentConfiguration;

    final private int cacheSize;

    final private int retryAfter; //retry configurations after x samples
    private int retryPhase = 0;

    //MOE hyper-parameter
    @NumericParameter(min=0.2, max=30)
    int lengthScaleDivider = 2;
    @NumericParameter(min=1, max=30)
    int numOptimizerMultistarts =  20;
    @NumericParameter(min=0.0001, max=2.0)
    double gaussianSignalVariance = 6.0;

    final private boolean useDefaultValues;

    private boolean initRandomSearchDone = false;

    final private boolean autoTimeMeasure;

    private OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(320, TimeUnit.SECONDS)
            .readTimeout(320, TimeUnit.SECONDS)
            .writeTimeout(320, TimeUnit.SECONDS)
            .build();

    private double bestResult = Double.MAX_VALUE;
    @Nullable private List<Double> bestConfiguration;
    @Nullable private T bestConfigurationObject;
    @Nullable private T currentConfigurationObject;
    private double currentConfigurationCosts;
    private long startTimeStamp = Long.MIN_VALUE;
    private long elapsedTime = 0;

    @NotNull
    final protected List<Field> numericFields;
    @NotNull
    final protected List<Field> nominalFields;

    @NotNull
    final protected String moeOptimizerUrl;

    public AutoTuneDefault(T config){
        super(config);
        logger.debug("Tuner created for configuration class " + config.getClass().getName());

        //extract class information
        this.cacheSize = tuneSettings.cacheNextPoints();
        this.autoTimeMeasure = tuneSettings.autoTimeMeasure();
        this.retryAfter = tuneSettings.reftryAfter();
        this.useDefaultValues = tuneSettings.sampleDefaultValues();

        this.numericFields = FieldUtils.getFieldsListWithAnnotation(config.getClass(), NumericParameter.class);

        this.nominalFields = FieldUtils.getFieldsListWithAnnotation(config.getClass(), NominalParameter.class);

        Map<String, String> env = System.getenv();
        if (env.containsKey("MOE_URL")) {
            moeOptimizerUrl = env.get("MOE_URL");
        } else {
            moeOptimizerUrl = "http://127.0.0.1:6543";
        }

    }

    @Override
    public AutoTune<T> start() {

        /** extract numeric field information **/
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
        req.getCovariance_info().getHyperparameters().add(gaussianSignalVariance); //add signal variance information, for the gaussian process
        for (Field field : nominalFields){
            NominalParameter nominalParameterInfo = field.getAnnotation(NominalParameter.class);

            GpNextPointsRequest.Domain boundA = new GpNextPointsRequest.Domain(nominalParameterInfo.values().length - 1, 0);
            req.getDomain_info().getDomain_bounds().add(boundA);

            //length scale for nominal value is one
            req.getCovariance_info().getHyperparameters().add(1.0);
        }

        req.getDomain_info().updateDimension();


        //random search at the begin
        if(!initRandomSearchDone){
            final int numberOfSamples = tuneSettings.initRandomSearch();
            cachedConfiguration = new ArrayList<>(numberOfSamples);
            logger.debug("Number of random samples: {}", numberOfSamples);

            //prepare cachedConfiguration
            for (int i = 0; i < numberOfSamples; i++) {
                cachedConfiguration.add(new ArrayList<>(numericFields.size()));
            }

            Random rand = new Random();
            //fill cachedConfiguration with random samples by dimension
            for (int i = 0; i < numericFields.size(); i++) { //for numericFields
                NumericParameter numericParameterInfo = numericFields.get(i).getAnnotation(NumericParameter.class);
                final double parameterWidth = (numericParameterInfo.max() - numericParameterInfo.min());

                for(int j = 0; j < numberOfSamples; j++){
                    cachedConfiguration.get(j).add(rand.nextDouble()*parameterWidth + numericParameterInfo.min());
                }
            }
            for (int i = 0; i < nominalFields.size(); i++) { //for nominalFields
                NominalParameter nominalParameterInfo = nominalFields.get(i).getAnnotation(NominalParameter.class);
                final double parameterWidth = (double) nominalParameterInfo.values().length - 1;

                for(int j = 0; j < numberOfSamples; j++){
                    cachedConfiguration.get(j).add(rand.nextDouble()*parameterWidth);
                }
            }

            initRandomSearchDone = true;

            if(useDefaultValues) {
                try {
                    //add default values for probing (numeric values)
                    ArrayList<Double> predefinedDefaultParameters = new ArrayList<>(numericFields.size() + nominalFields.size());
                    for (Field field : numericFields) {
                        predefinedDefaultParameters.add(field.getDouble(config));
                    }
                    for (Field field : nominalFields) {
                        NominalParameter nominalParameterInfo = field.getAnnotation(NominalParameter.class);
                        String strLabel;
                        if (field.getType().equals(boolean.class)) {
                            strLabel = String.valueOf(field.getBoolean(config));
                        } else {
                            strLabel = (String) field.get(config);
                        }
                        int i;
                        for (i = 0; i < nominalParameterInfo.values().length; i++) {
                            if (nominalParameterInfo.values()[i].equals(strLabel)) {
                                break;
                            }
                        }
                        predefinedDefaultParameters.add(new Double(i));
                    }
                    cachedConfiguration.add(predefinedDefaultParameters);
                }catch (IllegalAccessException exc){
                    logger.catching(exc);
                    throw new RuntimeException("Can't access the config object.", exc.getCause());
                }
            }

        }else if(cachedConfiguration.size() == 0){
            //fill cache from REST optimizer
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(this.moeOptimizerUrl)
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
        if (cachedConfiguration == null || cachedConfiguration.isEmpty()) {
            logger.warn("No new configuration found. Use best known configuration.");
            cachedConfiguration = new LinkedList<List<Double>>() {{
                add(bestConfiguration);
            }};
        }
        else {

            /** retry sampled configurations **/
            if (retryAfter != 0 && (sampledConfigurations.size() + 1) % retryAfter == 0 && (sampledConfigurations.size() + 1) / retryAfter > retryPhase) {
                //copy last cached configuration again to the top of the list
                cachedConfiguration.add(cachedConfiguration.get(cachedConfiguration.size() - 1));

                //add all x-1 last sampled configurations to cachedConfigurations but in reversed order
                List<List<Double>> sublist = sampledConfigurations.subList(retryPhase * retryAfter, retryPhase * retryAfter + retryAfter - 1)
                        .stream().map(listDoublePair -> listDoublePair.getKey()).collect(Collectors.toList());
                Collections.reverse(sublist);
                cachedConfiguration.addAll(sublist);

                retryPhase += 2;
            }


            currentConfiguration = cachedConfiguration.get(cachedConfiguration.size() - 1);
            cachedConfiguration.remove(cachedConfiguration.size() - 1);
        }
        logger.debug("New configuration values: {}", Arrays.toString(currentConfiguration.toArray()));


        currentConfigurationCosts = 0;

        try {
            Iterator<Double> currentConfigurationItr = currentConfiguration.iterator();
            for (Field field : numericFields) { //for numeric fields
                Double parameterValue = currentConfigurationItr.next();
                if (field.getType().equals(long.class)) {
                    field.setLong(config, parameterValue.longValue());
                } else if (field.getType().equals(int.class)) {
                    field.setInt(config, parameterValue.intValue());
                } else {
                    //assume it is a double parameter
                    field.setDouble(config, parameterValue);
                }
                NumericParameter numericParameterInfo = field.getAnnotation(NumericParameter.class);
                currentConfigurationCosts += parameterValue * numericParameterInfo.cost();

            }
            for (Field field : nominalFields) { //for nominal fields
                NominalParameter nominalParameterInfo = field.getAnnotation(NominalParameter.class);
                int label = (int) Math.round(currentConfigurationItr.next());
                label = label < 0 ? 0 : label;
                label = label >= nominalParameterInfo.values().length ? nominalParameterInfo.values().length - 1 : label;

                String strLabel = nominalParameterInfo.values()[label];

                if (field.getType().equals(boolean.class)) {
                    field.setBoolean(config, Boolean.parseBoolean(strLabel));
                } else {
                    //assume it is a String parameter
                    field.set(config, strLabel);
                }
            }
            this.currentConfigurationObject = config;
        }catch (IllegalAccessException exc){
            logger.catching(exc);
            throw new RuntimeException("Can't set value into config object", exc.getCause());
        }

        if(this.autoTimeMeasure){
            this.startTimeMeasure();
        }

        return this;
    }

    @Override
    public void end() {
        if(this.autoTimeMeasure){
            this.stopTimeMeasure();
        }

        double amount = currentConfigurationCosts; //add costs from configuration
        amount += elapsedTime;//add elapsed time as cost

        if(!Double.isFinite(amount)){
            //sanitize result
            amount = Double.MAX_VALUE;
        }
        if(amount < this.bestResult){
            //new best result
            this.bestResult = amount;
            this.bestConfiguration = currentConfiguration;
            this.bestConfigurationObject = currentConfigurationObject;
            logger.debug("New best configuration found! Cost value: {}", this.bestResult);
        }
        sampledConfigurations.add(new Pair<>(currentConfiguration, amount));

        this.currentConfigurationObject = null;
        this.elapsedTime = 0;
        logger.trace("Sampled configurations as CSV \n{}", this.getSampledConfigurationAsCSV());
    }

    @Override
    public @NotNull T getConfig(){
        if(this.currentConfigurationObject == null){
            throw new RuntimeException("You have to call start() first.");
        }
        return this.currentConfigurationObject;
    }

    @Override
    public @Nullable T getBestConfiguration() {
        return this.bestConfigurationObject;
    }

    @Override
    public List<Double> getBestConfigurationParameter() {
        return this.bestConfiguration;
    }

    @Override
    public double getBestResult() {
        if (logger.getLevel().equals(Level.DEBUG)) {
            logger.debug("Sampled configurations as CSV \n{}", this.getSampledConfigurationAsCSV());
        }
        return this.bestResult;
    }

    @Override
    public void startTimeMeasure() {
        if(this.startTimeStamp != Long.MIN_VALUE){
            throw new RuntimeException("Start time measure but time measure are already started!");
        }
        this.startTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void stopTimeMeasure() {
        if(this.startTimeStamp == Long.MIN_VALUE){
            throw new RuntimeException("End time measure but time measure yet not started!");
        }
        this.elapsedTime += System.currentTimeMillis()-this.startTimeStamp;
        this.startTimeStamp = Long.MIN_VALUE;
        logger.debug("Stop time measure. Elapsed time: {} ms", this.elapsedTime);
    }

    @Override
    public void addCost(double cost) {
        this.currentConfigurationCosts += cost;
    }

    public String getSampledConfigurationAsCSV() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Field nField : this.numericFields) {
            stringBuilder.append(nField.getName());
            stringBuilder.append(';');
        }
        for (Field nField : this.nominalFields) {
            stringBuilder.append(nField.getName());
            stringBuilder.append(';');
        }
        stringBuilder.append("cost");
        stringBuilder.append(';');
        stringBuilder.append("order");
        stringBuilder.append('\n');

        int counter = 0;
        for (Pair<List<Double>, Double> conf : sampledConfigurations) {

            stringBuilder.append(conf.getKey().stream().map(i -> String.format(Locale.GERMAN, "%f", i)).collect(Collectors.joining(";")));
            stringBuilder.append(';');

            stringBuilder.append(String.format(Locale.GERMAN, "%f", (conf.getValue())));
            stringBuilder.append(';');

            stringBuilder.append(counter++);
            stringBuilder.append('\n');
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1); //remove last new line

        return stringBuilder.toString();
    }

}
