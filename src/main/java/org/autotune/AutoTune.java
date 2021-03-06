package org.autotune;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Created by KevinRoj on 26.04.17.
 */
public abstract class AutoTune<T extends Serializable>{

    protected @NotNull T config;
    protected @NotNull
    TuneableParameters tuneSettings;

    /**
     * The fields of the given object will be optimized such as it was defined by the containing annotations
     *
     * @param config Object with annotated fields
     */
    public AutoTune(@NotNull T config){
        this.config = config;
        tuneSettings = config.getClass().getAnnotation(TuneableParameters.class);
    }

    /**
     * Generates the optimized config object
     * @return self reference to tuner
     */
    abstract public AutoTune<T> start();

    /**
     * Saves all benchmark results
     */
    abstract public void end();

    /**
     * Return a reference to the optimized config object
     * @return reference to config object
     */
    public abstract @NotNull
    T getConfig();

    /**
     * Returns the best known configuration
     *
     * @return config object
     */
    public abstract @Nullable
    T getBestConfiguration();

    /**
     * Returns the best known parameters as double values
     *
     * @return config object
     */
    public abstract List<Double> getBestConfigurationParameter();

    /**
     * The result from the best known configuration
     *
     * @return result as double
     */
    public abstract double getBestResult();

    /**
     * Start time measuring
     *
     * @return
     */
    public abstract void startTimeMeasure();

    /**
     * End time measuring and add it to internal cost function
     *
     * @return
     */
    public abstract void stopTimeMeasure();

    /**
     * Add manual the cost for the actual configuration
     *
     * @return
     */
    public abstract void addCost(double cost);

    static public class util{
        public final static String[] listTypes = {"ArrayList", "LinkedList"};

        static public @NotNull List getOptimizedList(@NotNull String listType){
            List list;
            switch (listType){
                case "ArrayList":
                    list = new ArrayList();
                    break;
                case "LinkedList":
                    list = new LinkedList();
                    break;
                default:
                    list = new ArrayList();
            }
            return list;
        }

        public final static String[] mapTypes = {"HashMap", "TreeMap", "Hashtable"};

        static public @NotNull
        Map getOptimizedMap(@NotNull String mapType) {
            Map map;
            switch (mapType) {
                case "HashMap":
                    map = new HashMap();
                    break;
                case "TreeMap":
                    map = new TreeMap();
                    break;
                case "Hashtable":
                    map = new Hashtable();
                    break;
                default:
                    map = new HashMap();
            }
            return map;
        }
    }
}
