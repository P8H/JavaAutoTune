import org.autotune.AutoTune;
import org.autotune.AutoTuneDefault;
import org.autotune.NumericParameter;
import org.autotune.TuneableParameters;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by KevinRoj on 26.04.17.
 */

class TunePolynomialFunctionTest {

    @TuneableParameters(initRandomSearch = 5)
    class VariableXYZ implements Serializable {
        static final long serialVersionUID = 421L;
        @NumericParameter(min=0, max=100)
        double x  = 1;
        @NumericParameter(min=1, max=100)
        double y = 1;
        @NumericParameter(min=1, max=100)
        double z = 1;
    }

    @org.junit.jupiter.api.Test
    double simpleProductTest() throws IllegalAccessException {
        AutoTune<VariableXYZ> tuner = new AutoTuneDefault(new VariableXYZ());

        for (int i = 0; i < 30; i++) {
            VariableXYZ cfg = tuner.getConfig();
            double value = cfg.x*cfg.y*cfg.z;
            //tuner.setResult(value);
            System.out.printf("Finished iteration %d with value %f \n", i, value);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());

        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));

        return tuner.getBestResult();
    }

    @org.junit.jupiter.api.Test
    double simplePolynomTest() throws IllegalAccessException {
        AutoTune<VariableXYZ> tuner = new AutoTuneDefault(new VariableXYZ());

        for (int i = 0; i < 30; i++) {
            VariableXYZ cfg = tuner.getConfig();
            double value = Math.sqrt(cfg.z)*cfg.x + 999/(cfg.y+1);
            //tuner.setResult(value);
            System.out.printf("Finished iteration %d with value %f \n", i, value);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());

        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));

        return tuner.getBestResult();
    }

    @TuneableParameters(initRandomSearch = 15)
    class ManyVariables implements Serializable {
        static final long serialVersionUID = 421L;
        @NumericParameter(min=0, max=100)
        double x  = 1;
        @NumericParameter(min=1, max=100)
        double y = 1;
        @NumericParameter(min=1, max=100)
        double z = 1;
        @NumericParameter(min=0, max=50)
        double x2  = 11;
        @NumericParameter(min=1, max=100)
        double y2 = 11;
        @NumericParameter(min=1, max=100)
        double z2 = 11;
        @NumericParameter(min=0, max=42)
        double x3  = 1;
        @NumericParameter(min=1, max=100)
        double y3 = 1;
        @NumericParameter(min=1, max=1000)
        double z3 = 1;
        @NumericParameter(min=0, max=100)
        double x4  = 1;
        @NumericParameter(min=1, max=100)
        double y4 = 1;
        @NumericParameter(min=1, max=10000)
        double z4 = 1;
        @NumericParameter(min=0, max=100)
        double x5  = 144;
        @NumericParameter(min=1, max=100)
        double y5 = 1;
        @NumericParameter(min=1, max=10)
        double z5 = 1;
        @NumericParameter(min=0.5, max=1.9)
        double x6  = 1;
        @NumericParameter(min=1, max=100)
        double y6 = 13;
        @NumericParameter(min=1, max=100)
        double z6 = 1;
    }

    @org.junit.jupiter.api.Test
    double longPolynomTest() throws IllegalAccessException {
        AutoTune<ManyVariables> tuner = new AutoTuneDefault(new ManyVariables());

        for (int i = 0; i < 100; i++) {
            ManyVariables cfg = tuner.getConfig();
            double value = Math.sqrt(cfg.z)*cfg.x + 999/(cfg.y+1);
            value += Math.sqrt(cfg.z2)*cfg.x2 + 999/(cfg.y2+1);
            value += Math.sqrt(cfg.z3)*cfg.x3*cfg.x3 + 30/(cfg.y3+5);
            value += Math.sqrt(cfg.z4)*cfg.x4*cfg.x4 + 30/(cfg.y4+5);
            value += Math.sqrt(cfg.z5) + cfg.x5*cfg.x5 + cfg.y5;
            value += cfg.z6*cfg.x6*cfg.y6;
            //tuner.setResult(value);
            System.out.printf("Finished iteration %d with value %f \n", i, value);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());

        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));

        return tuner.getBestResult();
    }

}