import org.autotune.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by KevinRoj on 26.04.17.
 */

class SimpleRealTest {
    @TuneableParameters(initRandomSearch = 3, cacheNextPoints = 1, autoTimeMeasure = true)
    public class OneParameter implements Serializable {
        static final long serialVersionUID = 4213L;
        @NumericParameter(min = 1, max = 100000, cost = 0.5)
        public int inputBufferSize = 86915;
    }

    @org.junit.jupiter.api.Test
    void simpleBufferTest() throws IOException, IllegalAccessException, InterruptedException {
        AutoTune<OneParameter> tuner = new AutoTuneDefault(new OneParameter());

        //warm up
        {
            for (int j = 0; j < 5; j++) {
                BufferedReader reader = new BufferedReader(new FileReader("datasets/rita_flight/rita_flight_2008.csv"));
                long ff = reader.lines().count();
                reader.close();
            }
        }

        for (int i = 1; i < 13; i++) {
            OneParameter cfg = tuner.start().getConfig();

            BufferedReader reader = new BufferedReader(new FileReader("datasets/rita_flight/rita_flight_2008.csv"), cfg.inputBufferSize);
            long ff = reader.lines().count();
            tuner.end();
            reader.close();

            //clear disk cache on unix
            //echo 3 > /proc/sys/vm/drop_caches
            Runtime r = Runtime.getRuntime();
            //clear disk cache on OS X
            Process p = r.exec("sync && sudo purge");
            p.waitFor();

            System.out.println(cfg.inputBufferSize);
        }
        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());
        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));

        long start = System.currentTimeMillis();
        BufferedReader reader  = new BufferedReader(new FileReader("datasets/rita_flight/rita_flight_2008.csv"));
        long ff = reader.lines().count();
        System.out.printf("Result with default value %d \n", System.currentTimeMillis() - start);
        System.out.printf("Improvement %f %% \n", ((System.currentTimeMillis() - start) / (tuner.getBestResult()) * 100 - 100));
    }

    @TuneableParameters(initRandomSearch = 2, reftryAfter = 2, cacheNextPoints = 2, autoTimeMeasure = true)
    public class OptimizedList implements Serializable {
        static final long serialVersionUID = 4213L;
        @NominalParameter(values = {"ArrayList", "LinkedList"})
        public String list1Type = AutoTune.util.listTypes[0];
    }
    @org.junit.jupiter.api.Test
    void simpleOptimizedListTest() throws IllegalAccessException {
        AutoTune<OptimizedList> tuner = new AutoTuneDefault(new OptimizedList());

        for (int t = 0; t < 10; t++) { //10 benchmark tests
            OptimizedList cfg = tuner.start().getConfig();
            List<Double> list1 = AutoTune.util.getOptimizedList(cfg.list1Type);

            //Random rndGen = new Random();
            for (int i = 0; i < 50000; i++) {
                list1.add(1.0);
            }
            for (int i = 0; i < 50000 / 3; i++) {
                list1.remove(i * 2);
            }
            tuner.end();
            System.out.println(cfg.list1Type);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());
        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));
        System.out.println(tuner.getBestConfiguration().list1Type);

    }

    @TuneableParameters(initRandomSearch = 3, reftryAfter = 2, cacheNextPoints = 2, autoTimeMeasure = true)
    public class OptimizedMap implements Serializable {
        static final long serialVersionUID = 4213L;
        @NominalParameter(values = {"HashMap", "TreeMap", "Hashtable"})
        public String map1Type = AutoTune.util.mapTypes[0];
    }

    @org.junit.jupiter.api.Test
    void simpleOptimizedMapTest() {

        AutoTune<OptimizedMap> tuner = new AutoTuneDefault(new OptimizedMap());

        for (int t = 0; t < 20; t++) { //10 benchmark tests
            OptimizedMap cfg = tuner.start().getConfig();
            Map<String, Double> map1 = AutoTune.util.getOptimizedMap(cfg.map1Type);

            Random rndGen = new Random();
            for (int i = 0; i < 500000; i++) {
                map1.put(Long.toString(rndGen.nextInt()), rndGen.nextDouble());
            }
            for (int i = 0; i < 50000; i++) {
                map1.remove(Long.toString(rndGen.nextInt()));
            }
            tuner.end();
            System.out.println(cfg.map1Type);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());
        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));
        System.out.println(tuner.getBestConfiguration().map1Type);

    }

}