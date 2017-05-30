import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.autotune.AutoTune;
import org.autotune.AutoTuneDefault;
import org.autotune.exampleConfigs.SparkTuneableConf;
import scala.Tuple2;

import static org.apache.spark.sql.functions.*;

/**
 * Created by KevinRoj on 26.04.17.
 */

class SparkTest {
    private SparkSession.Builder coreSparkBuilder(){
        return SparkSession
                .builder()
                .appName("Java Spark SQL data sources example");
    }//.master("spark://141.100.62.105:7077"))


    private void reduceLogLevel(SparkSession spark){
        JavaSparkContext sparkContextD = JavaSparkContext.fromSparkContext(spark.sparkContext());
        sparkContextD.setLogLevel("ERROR");
    }

    @org.junit.jupiter.api.Test
    void littleTest() throws InterruptedException {
        AutoTune<SparkTuneableConf> tuner = new AutoTuneDefault(new SparkTuneableConf());
        {
            //warm up
            SparkTuneableConf cfgDefault = new SparkTuneableConf(); //with default values

            SparkSession sparkD = cfgDefault.setConfig(coreSparkBuilder()).getOrCreate();
            reduceLogLevel(sparkD);

            simpleSparkMethod(sparkD);
            sparkD.stop();
        }

        for (int t = 0; t < 40; t++) { //40 benchmark tests
            SparkTuneableConf cfg = tuner.start().getConfig();
            SparkSession spark = cfg.setConfig(coreSparkBuilder()).getOrCreate();
            reduceLogLevel(spark);

            tuner.startTimeMeasure();

            simpleSparkMethod(spark);

            tuner.stopTimeMeasure();

            spark.stop();

            tuner.end();
        }

        {
            //Get best configuration an wait
            SparkTuneableConf cfg = tuner.getBestConfiguration();
            SparkSession spark = cfg.setConfig(coreSparkBuilder()).getOrCreate();

            reduceLogLevel(spark);

            simpleSparkMethod(spark);

            System.out.println("Best configuration with result:" + tuner.getBestResult());
            Thread.sleep(1000 * 60 * 10); // wait a little bit
        }
    }

    void simpleSparkMethod(SparkSession spark) {
        Dataset<Row> dataset1 = spark.read().format("csv").option("header", true).option("inferSchema", false).load("datasets/rita_flight/rita_flight_2008.csv");
        //Dataset<Row> dataset2 = spark.read().format("csv").option("header", true).option("inferSchema", false).load("datasets/rita_flight/rita_flight_2007.csv");
        //Dataset<Row> dataset3 = spark.read().format("csv").option("header", true).option("inferSchema", false).load("datasets/rita_flight/rita_flight_2006.csv");
        //dataset1 = dataset1.union(dataset2).union(dataset3);

        Dataset<Row> arrivalDelay = dataset1.select("Dest", "ArrDelay")
                .map(value -> {
                    int arrDelay;
                    try {
                        arrDelay = Integer.parseInt(value.getString(1));
                    } catch (NumberFormatException e) {
                        arrDelay = 0;
                    }
                    return new Tuple2<>(value.getString(0), arrDelay);
                }, Encoders.tuple(Encoders.STRING(), Encoders.INT()))
                .toDF("Dest", "ArrDelay")
                .groupBy("Dest")
                .agg(mean("ArrDelay").alias("meanDelay"), max("ArrDelay").alias("maxDelay"), sum("ArrDelay").alias("sumDelay"))
                .sort(desc("meanDelay"), desc("maxDelay"))
                .cache();
        //arrivalDelay.show();
        //arrivalDelay.printSchema();
    }
}