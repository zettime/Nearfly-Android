#### 20thesis04 - Alexis dos Santos

# Nearfly-Android

Entwicklung einer Android-Bibliothek, welche Nearby Connections und Mqtt wrappt.



Die Nearfly-Android-Bibliothek, kann wie folgt eingebunden werden:

1.  Die Passende AAR-Datei aus dem Ordner "AndroidLib" in das libs (`<Projectname>/app/libs/`)Verzeichnis des eigenen Projektes Kopieren

2. Die Bibliothek hinzufügen der Abhängigkeiten in die build.gradle auf Modulebene(z.B. app)  einbinden: 

   ```java
   android {
       compileOptions {
           sourceCompatibility JavaVersion.VERSION_1_8
           targetCompatibility JavaVersion.VERSION_1_8
       }
   }
   
   repositories {
       flatDir {
           dirs 'libs'
       }
   }
   
   dependencies {
       implementation 'de.pbma.nearfly:nearfly-latest@aar'
       
       // May not be needed in the future
       implementation 'com.google.android.gms:play-services-nearby:17.0.0'
       implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
   }
   
   ```

3. In das Manifest, sowohl den Nearfly-Service, wie auch die nötigen Berechtigungen eintragen:

   

   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android">
   
       <!-- Required for Nearby Connections -->
       <uses-permission android:name="android.permission.BLUETOOTH" />
       <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
       <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
       <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
       <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
       <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
       
       <!-- Required for MQTT -->
       <uses-permission android:name="android.permission.INTERNET" />
       
       <!-- Optional: Only required if File transfer API used -->
       <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE " />
       <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   
       <application
           <activity
           <!-- .... -->
           </activity>
   
           <service
               android:name="de.pbma.nearfly.NearflyService"
               android:enabled="true" />
       </application>
   </manifest>
   ```

