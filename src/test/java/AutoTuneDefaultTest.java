import java.io.Serializable;

/**
 * Created by KevinRoj on 26.04.17.
 */

class AutoTuneDefaultTest{
    class Task1Config implements Serializable {
        static final long serialVersionUID = 421L;
        @NumericParameter
        int PARAM1  = 1;
        @NumericParameter(min=0.1, max=2.5)
        double PARAM2 = 0.1;
        @NumericParameter
        double PARAM3 = 0.2;
        public double setX(){
            return PARAM2;
        }
    }


    @org.junit.jupiter.api.BeforeEach
    void setUp() {
    }

    @org.junit.jupiter.api.Test
    void littleTest() throws IllegalAccessException {
        AutoTuneDefault<Task1Config> tuner = new AutoTuneDefault(new Task1Config());
        Task1Config cfg = tuner.getConfig();
        tuner.setResult(3);
    }



}