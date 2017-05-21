package org.autotune.exampleConfigs;

import org.apache.spark.sql.SparkSession;
import org.autotune.NominalParameter;
import org.autotune.NumericParameter;
import org.autotune.TuneableParameters;

import java.io.Serializable;

/**
 * Created by KevinRoj on 21.05.17.
 */
@TuneableParameters(initRandomSearch = 1, cacheNextPoints = 1)
public class SparkTuneableConf implements Serializable {
    static final long serialVersionUID = 421L;
    @NumericParameter(min = 13421772, max = 671088640)
    public int maxPartitionBytes = 134217728;
    @NominalParameter(values = {"false", "true"})
    public boolean inMemoryColumnarStorageCompressed = true;
    @NumericParameter(min = 2, max = 500)
    public int shufflePartitions = 200;
    @NominalParameter(values = {"false", "true"})
    public boolean shuffleCompress = true;
    @NominalParameter(values = {"false", "true"})
    public boolean shuffleSpillCompress = true;
    @NominalParameter(values = {"false", "true"})
    public boolean broadcastCompress = true;
    @NominalParameter(values = {"false", "true"})
    public boolean rddCompress = false;
    @NumericParameter(min = 1, max = 8)
    public int defaultParallelism = 2;
    @NumericParameter(min = 1, max = 8)
    public long executorCores = 2;
    @NumericParameter(min = 1, max = 4)
    public long taskCpus = 1;

    public SparkSession.Builder setConfig(SparkSession.Builder builder) {
        return builder
                .config("spark.sql.inMemoryColumnarStorage.compressed", this.inMemoryColumnarStorageCompressed)
                .config("spark.sql.files.maxPartitionBytes", this.maxPartitionBytes)
                .config("spark.sql.shuffle.partitions", this.shufflePartitions)
                .config("spark.shuffle.compress", this.shuffleCompress)
                .config("spark.shuffle.spill.compress", this.shuffleSpillCompress)
                .config("spark.broadcast.compress", this.broadcastCompress)
                .config("spark.rdd.compress", this.rddCompress)
                .config("spark.default.parallelism", this.defaultParallelism)
                .config("spark.executor.cores", this.executorCores)
                .config("spark.task.cpus", this.taskCpus);
    }
}
