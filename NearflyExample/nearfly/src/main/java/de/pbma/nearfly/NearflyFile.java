package de.pbma.nearfly;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;

public class NearflyFile {
    private String filename;
    private String extentsion;
    private Uri uri;

    public NearflyFile(Context context, Uri uri){
        this.uri = uri;
    }

    public String getFilename(){
        return filename;
    }

    public String getExtentsion(){
        return extentsion;
    }

    public Uri getUri(){
        return uri;
    }
}
