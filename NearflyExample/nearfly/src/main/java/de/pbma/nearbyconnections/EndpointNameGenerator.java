package de.pbma.nearbyconnections;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

class EndpointNameGenerator {

    public String generateRandomName_if_not_in_sharedPref(Context context) {
        String endpointName;

        SharedPreferences sharedPreferences
                = context.getSharedPreferences("NearflyEndpointName", Context.MODE_PRIVATE);
        endpointName = sharedPreferences.getString("endpointName", null);

        if (endpointName == null) {
            SharedPreferences.Editor sharedPrefEditor = sharedPreferences.edit();
            endpointName = generateRandomName();
            sharedPrefEditor.putString("endpointName", endpointName);
        }
        return endpointName;
    }

    public String getNamePendentFromTime(){
            final long MAX_DAYTIME = (60*60*1000*24);

            int num = getMaxCPUFreqMHz();
            long time = MAX_DAYTIME-(currMillis()%MAX_DAYTIME);
            long DURATION = 20;
            // Bei 1300 zu 1700 Zeit*4 bei 1500 zu 1700 Zeit*2
            return ""+(time+(num*10*DURATION));
    }

    private long currMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Use this to get the Max CPU Frequence in MHz
     *
     * @return cpu frequency in MHz
     */
    public int getMaxCPUFreqMHz() {
        int maxFreq = -1;
        try {

            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state", "r");

            boolean done = false;
            while (!done) {
                String line = reader.readLine();
                if (null == line) {
                    done = true;
                    break;
                }
                String[] splits = line.split("\\s+");
                assert (splits.length == 2);
                int timeInState = Integer.parseInt(splits[1]);
                if (timeInState > 0) {
                    int freq = Integer.parseInt(splits[0]) / 1000;
                    if (freq > maxFreq) {
                        maxFreq = freq;
                    }
                }
            }

        } catch (IOException ex) {
            // ex.printStackTrace();
        }

        return maxFreq;
    }

    /**
     * Create a random Name
     *
     * @return Random Name
     */
    private String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return /*TODO*/getMaxCPUFreqMHz() + " " + name;
    }
}
