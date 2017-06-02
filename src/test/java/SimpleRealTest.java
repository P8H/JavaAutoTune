import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.autotune.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by KevinRoj on 26.04.17.
 */

class SimpleRealTest {

    @TuneableParameters(initRandomSearch = 5, autoTimeMeasure = true)
    public class OptimalParameter implements Serializable {
        static final long serialVersionUID = 42122L;
        @NumericParameter(min=1, max=100000)
        int inputBufferSize = 6000;
    }

    @TuneableParameters(initRandomSearch = 3, cacheNextPoints = 2, autoTimeMeasure = true)
    public class OneParameter implements Serializable {
        static final long serialVersionUID = 4213L;
        @NumericParameter(min=1, max=100000, cost=6000)
        int inputBufferSize = 86915;
    }

    @org.junit.jupiter.api.Test
    void stupidBufferTest() throws IOException, IllegalAccessException, InterruptedException {
        AutoTune<OneParameter> tuner = new AutoTuneDefault(new OneParameter());
        for (int i = 1; i < 13; i++) {
            long duration = 0;
            OneParameter cfg = tuner.start().getConfig();
            for(int j = 0; j < 3; j++) {
                BufferedReader reader = new BufferedReader(new FileReader("datasets/rita_flight/rita_flight_2008 Kopie " + i +".csv"), cfg.inputBufferSize);
                long ff = reader.lines().count();
                reader.close();

                //clear disk cache on unix
                //echo 3 > /proc/sys/vm/drop_caches
                Runtime r = Runtime.getRuntime();
                //clear disk cache on OS X
                Process p = r.exec("sync && sudo purge");
                p.waitFor();

            }
            //tuner.setResult(duration/3);
            tuner.end();
            System.out.printf("Average time: %d \n", duration/3);
            System.out.println(cfg.inputBufferSize);
        }
        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());
        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));

        long start = System.nanoTime();
        BufferedReader reader  = new BufferedReader(new FileReader("datasets/rita_flight/rita_flight_2008.csv"));
        long ff = reader.lines().count();
        System.out.printf("Result with default value %d \n", System.nanoTime()-start);
        System.out.printf("Improvement %f %% \n", ((System.nanoTime()-start)/tuner.getBestResult()*100));
    }

    @TuneableParameters(initRandomSearch = 3, cacheNextPoints = 2, autoTimeMeasure = true)
    public class OptimizedList implements Serializable {
        static final long serialVersionUID = 4213L;
        @NominalParameter(values = {"ArrayList", "LinkedList"})
        public String list1Type = AutoTune.util.listTypes[0];
    }
    @org.junit.jupiter.api.Test
    void simpleOptimizedListAddTest() throws IllegalAccessException {
        AutoTune<OptimizedList> tuner = new AutoTuneDefault(new OptimizedList());

        for (int t = 0; t < 10; t++) { //10 benchmark tests
            OptimizedList cfg = tuner.start().getConfig();
            List<Double> list1 = AutoTune.util.getOptimizedList(cfg.list1Type);

            //Random rndGen = new Random();
            for (int i = 0; i < 5000000; i++) {
                list1.add(1.0);
            }
            tuner.end();
            System.out.println(cfg.list1Type);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());
        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));
        System.out.println(tuner.getBestConfiguration().list1Type);

    }

    @TuneableParameters(initRandomSearch = 3, cacheNextPoints = 2, autoTimeMeasure = true)
    public class OptimizedMap implements Serializable {
        static final long serialVersionUID = 4213L;
        @NominalParameter(values = {"HashMap", "TreeMap", "Hashtable"})
        String map1Type = AutoTune.util.mapTypes[0];
    }

    @org.junit.jupiter.api.Test
    void simpleMapTest() {

        AutoTune<OptimizedMap> tuner = new AutoTuneDefault(new OptimizedMap());

        for (int t = 0; t < 10; t++) { //10 benchmark tests
            OptimizedMap cfg = tuner.start().getConfig();
            Map<String, Double> map1 = AutoTune.util.getOptimizedMap(cfg.map1Type);

            Random rndGen = new Random();
            for (int i = 0; i < 5000000; i++) {
                map1.put(Long.toString(rndGen.nextInt()), rndGen.nextDouble());
            }
            tuner.end();
            System.out.println(cfg.map1Type);
        }

        System.out.printf("Best configuration found with result %f \n", tuner.getBestResult());
        System.out.println(Arrays.toString(tuner.getBestConfigurationParameter().toArray()));
        System.out.println(tuner.getBestConfiguration().map1Type);

    }

    @org.junit.jupiter.api.Test
    void flightDataTest() throws IllegalAccessException, IOException, InterruptedException {
        //AutoTune<OptimalParameter> tuner = new AutoTuneDefault(new OptimalParameter());
        //OptimalParameter cfg = tuner.getConfig();

        /*
        final FileChannel channel = new FileInputStream("/Volumes/USB DISK/rita_flight_2008.csv").getChannel();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

        Reader reader = new BufferedReader(new InputStreamReader(new ByteBufferInputStream(buffer)), 5);
        */
        Reader reader  = new BufferedReader(new FileReader("/Volumes/USB DISK/rita_flight_2008.csv"), 1);

        CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withFirstRecordAsHeader());

        Map<String, AirportDelayEntry> delaysByAirport = new HashMap<>();
        for (CSVRecord csvRecord : parser) {
            String airport = csvRecord.get("Dest"); //destination IATA airport code
            String arrDelayStr = csvRecord.get("ArrDelay"); //arrival delay, in minutes

            int arrivalDelay = arrDelayStr.equals("NA") ? 0 : Integer.valueOf(arrDelayStr);

            if(delaysByAirport.containsKey(airport)){
                delaysByAirport.get(airport).addNewDelay(arrivalDelay);
            }else{
                delaysByAirport.put(airport, new AirportDelayEntry(arrivalDelay));
            }

        }

    //tuner.getBestResult();
    }

    @org.junit.jupiter.api.Test
    void flightDataMultiThreadedTest() throws InterruptedException, ExecutionException {
        final int threadCount = 2;

        // BlockingQueue with a capacity of 200
        BlockingQueue<CSVRecord> queue = new ArrayBlockingQueue<>(10);

        // create thread pool with given size
        ExecutorService service = Executors.newFixedThreadPool(threadCount);

        Map<String, AirportDelayEntry> delaysByAirport = new ConcurrentHashMap<>(10);

        for (int i = 0; i < (threadCount - 1); i++) {
            service.submit(new CPUTask(queue, delaysByAirport));
        }

        // Wait til FileTask completes
        service.submit(new FileTask(queue)).get();

        service.shutdownNow();  // interrupt CPUTasks

        // Wait til CPUTasks terminate
        service.awaitTermination(365, TimeUnit.DAYS);
    }



    private class AirportDelayEntry{
        int flights = 0;
        double averageArrivalDelay = 0.0;

        AirportDelayEntry(int firstArrivalDelay){
            this.averageArrivalDelay = firstArrivalDelay;
            this.flights = 1;
        }

        public void addNewDelay(int arrivalDelay){
            averageArrivalDelay = averageArrivalDelay*(flights/(flights+1)) + arrivalDelay*(1/flights+1);
            flights++;
        }
    }

    class FileTask implements Runnable {

        private final BlockingQueue<CSVRecord> queue;

        public FileTask(BlockingQueue<CSVRecord> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/Volumes/USB DISK/rita_flight_2008.csv"), 100000);
                CSVParser parser = new CSVParser(br, CSVFormat.RFC4180.withFirstRecordAsHeader());
                Iterator<CSVRecord> itr = parser.iterator();
                while (itr.hasNext()) {
                    // block if the queue is full
                    queue.put(itr.next());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CPUTask implements Runnable {

        private final BlockingQueue<CSVRecord> queue;
        private final Map<String, AirportDelayEntry> delaysByAirport;

        public CPUTask(BlockingQueue<CSVRecord> queue, Map<String, AirportDelayEntry> delaysByAirport) {
            this.queue = queue;
            this.delaysByAirport = delaysByAirport;
        }

        @Override
        public void run() {
            while(true) {
                try {
                    // block if the queue is empty
                    CSVRecord csvRecord = queue.take();
                    String airport = csvRecord.get("Dest"); //destination IATA airport code
                    String arrDelayStr = csvRecord.get("ArrDelay"); //arrival delay, in minutes

                    int arrivalDelay = arrDelayStr.equals("NA") ? 0 : Integer.valueOf(arrDelayStr);

                    if(delaysByAirport.containsKey(airport)){
                        delaysByAirport.get(airport).addNewDelay(arrivalDelay);
                    }else{
                        delaysByAirport.put(airport, new AirportDelayEntry(arrivalDelay));
                    }

                } catch (InterruptedException ex) {
                    break; // FileTask has completed
                }
            }
            /*
            // poll() returns null if the queue is empty
            while((line = queue.poll()) != null) {
                // do things with line;
            }
            */
        }
    }

    private class ByteBufferInputStream extends InputStream {
        ByteBuffer buf;

        public ByteBufferInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }


}