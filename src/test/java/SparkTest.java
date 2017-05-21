import org.apache.spark.api.java.JavaSparkContext;
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
    //TODO latin hypercube sample reparieren

    @org.junit.jupiter.api.Test
    void littleTest() throws InterruptedException {
        AutoTune<SparkTuneableConf> tuner = new AutoTuneDefault(new SparkTuneableConf());

        //warm up
        SparkTuneableConf cfgDefault = new SparkTuneableConf(); //with default values

        SparkSession sparkD = cfgDefault.setConfig(SparkSession
                .builder()
                .appName("Java Spark SQL data sources example")
                .master("local[*]"))
                .getOrCreate();

        JavaSparkContext sparkContextD = JavaSparkContext.fromSparkContext(sparkD.sparkContext());
        sparkContextD.setLogLevel("ERROR");
        simpleSparkMethod(sparkD);
        sparkD.stop();

        for (int t = 0; t < 40; t++) { //40 benchmark tests
            SparkTuneableConf cfg = tuner.start().getConfig();
            SparkSession spark = cfg.setConfig(SparkSession
                    .builder()
                    .appName("Java Spark SQL data sources example")
                    .master("local[*]"))
                    .getOrCreate();

            JavaSparkContext sparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());
            sparkContext.setLogLevel("ERROR");

            tuner.startTimeMeasure();

            simpleSparkMethod(spark);

            tuner.stopTimeMeasure();

            spark.stop();

            tuner.end();
        }

        //Get best configuration an wait
        SparkTuneableConf cfg = tuner.getBestConfiguration();
        SparkSession spark = cfg.setConfig(SparkSession
                .builder()
                .appName("Java Spark SQL data sources example")
                .master("local[*]"))
                .getOrCreate();

        JavaSparkContext sparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());
        sparkContext.setLogLevel("ERROR");


        simpleSparkMethod(spark);
        System.out.println("\007");
        System.out.println("Best configuration with result:" + tuner.getBestResult());
        Thread.sleep(1000 * 60 * 10);
    }

    void simpleSparkMethod(SparkSession spark) {
        Dataset<Row> usersDF = spark.read().format("csv").option("header", true).option("inferSchema", false).load("datasets/rita_flight/rita_flight_2008 Kopie 1.csv");

        Dataset<Row> usersDF2 = spark.read().format("csv").option("header", true).option("inferSchema", false).load("datasets/rita_flight/rita_flight_2008 Kopie 2.csv");
        usersDF = usersDF.union(usersDF2);

        System.out.println("Lines number: " + usersDF.count());
        //usersDF.printSchema();
        //usersDF.show();


        Dataset<Row> arrivalDelay = usersDF.select("Dest", "ArrDelay")
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