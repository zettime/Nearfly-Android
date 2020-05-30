package de.pbma.nearflyexample.measureTimes;

import android.icu.text.SimpleDateFormat;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import de.pbma.nearfly.Constants;

public class Logger {
    private Date today = Calendar.getInstance().getTime();
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
    private File filePath;
    private FileWriter writer = null;

    public Logger(String name) {
        File folder = new File(Constants.fileDirectory +
                File.separator + name);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }

        filePath = new File(Constants.fileDirectory+File.separator + name,
                formatter.format(today) + ".txt");
        try {
            writer = new FileWriter(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String str){
        try {
            writer.append(str);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}