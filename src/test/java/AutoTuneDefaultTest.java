import org.autotune.AutoTuneDefault;
import org.autotune.NominalParameter;
import org.autotune.NumericParameter;
import org.autotune.TuneableParameters;

import java.io.Serializable;

/**
 * Created by KevinRoj on 26.04.17.
 */

class AutoTuneDefaultTest{
    @TuneableParameters(initRandomSearch = 2, cacheNextPoints = 1)
    class Task1Config implements Serializable {
        static final long serialVersionUID = 421L;
        @NumericParameter
        int PARAM1  = 1;
        @NumericParameter(min=0.1, max=2.5)
        double PARAM2 = 0.1;
        @NominalParameter(values = {"val1", "val2"})
        String PARAM3 = "val1";
        @NominalParameter(values = {"true", "false"})
        boolean PARAM4 = false;
    }

    @org.junit.jupiter.api.Test
    void littleTest() throws IllegalAccessException {
        AutoTuneDefault<Task1Config> tuner = new AutoTuneDefault(new Task1Config());
        Task1Config cfg = tuner.getConfig();
        double result = 0;
        result += cfg.PARAM1;
        result += cfg.PARAM2;
        result += cfg.PARAM3.equals("val2") ? 0 : 1;
        result += cfg.PARAM4 ? 0 : 1;
        //tuner.setResult(result);
    }
}