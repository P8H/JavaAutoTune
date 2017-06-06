import org.autotune.*;

import java.io.Serializable;

/**
 * Created by KevinRoj on 26.04.17.
 * Functional test of AutoTune implementation
 */

class AutoTuneDefaultTest{
    @TuneableParameters(initRandomSearch = 2, cacheNextPoints = 1)
    public class Task1Config implements Serializable {
        static final long serialVersionUID = 421L;
        @NumericParameter
        public int PARAM1 = 1;
        @NumericParameter(min=0.1, max=2.5)
        public double PARAM2 = 0.1;
        @NominalParameter(values = {"val1", "val2"})
        public String PARAM3 = "val1";
        @NominalParameter(values = {"true", "false"})
        public boolean PARAM4 = false;
    }

    @org.junit.jupiter.api.Test
    void littleTest() throws InterruptedException {
        AutoTune<Task1Config> tuner = new AutoTuneDefault(new Task1Config());
        Task1Config cfg = tuner.start().getConfig();
        tuner.startTimeMeasure();
        double result = 0;
        result += cfg.PARAM1;
        result += cfg.PARAM2;
        result += cfg.PARAM3.equals("val2") ? 0 : 1;
        result += cfg.PARAM4 ? 0 : 1;
        Thread.sleep((long) result);
        tuner.stopTimeMeasure();
        tuner.end();
    }
}