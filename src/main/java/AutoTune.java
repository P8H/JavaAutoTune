import java.io.Serializable;
import java.util.List;

/**
 * Created by KevinRoj on 26.04.17.
 */
public abstract class AutoTune<T extends Serializable>{

    protected T config;

    /**
     * The fields of the given object will be optimized such as it was defined by the containing annotations
     *
     * Has to be the first call
     *
     * @param config Object with annotated fields
     */
    public AutoTune(T config){
        this.config = config;
    }

    /**
     * Return a reference to the optimized config object
     *
     * @throws IllegalAccessException
     */
    abstract T getConfig() throws IllegalAccessException;

    /**
     * Set the evaluated result from tuned config
     *
     * Only available after getConfig().
     *
     * @param amount lower is better
     */
    abstract void setResult(double amount);

    /**
     * Returns the best known configuration
     *
     * @return config object
     */
    abstract T getBestConfiguration();

    /**
     * Returns the best known parameters as double values
     *
     * @return config object
     */
    abstract List<Double> getBestConfigurationParameter();

    /**
     * The result from the best known configuration
     *
     * @return result as double
     */
    abstract double getBestResult();
}
