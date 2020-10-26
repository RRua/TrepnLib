package uminho.di.greenlab.trepnlibrary;

/**
 * Created by ruirua on 07/02/2017.
 */

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.Scanner;


public class TrepnLib {

    static Context context ; // we try to ensure that this reference is indeed lost to prevent memory leaks
    static int globalState = 0;
    static int qtdTraces = 0;
    static int qtdMeasures =0;
    static final String trepTag = "[TrepnLib]";
    static final String csvFilename = "GreendroidResultTrace";
    static final String dbFilename = "";
    static final int coolDownTimeMilis = Conventions.coolDownTimeMilis;
    static final int warmUpTimeMilis = Conventions.warmUpTimeMilis;

    public static Context initContext(){
        Context ctx = null;
        if (context==null){
            try {
                Application app = getApplicationUsingReflection();
                context = app.getApplicationContext();
                return app.getApplicationContext();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ctx;
    }

    public static  void loadPreferences (Context ctx, String preferencesFilename){
        Intent loadPreferences = new Intent("com.quicinc.trepn.load_preferences");
        loadPreferences.putExtra("com.quicinc.trepn.load_preferences_file",preferencesFilename);
        ctx.sendBroadcast(loadPreferences);
    }


    /*
    * For method oriented monitoring
    * */
    public static void startProfiling(Context ctx){ // potentially can receive a String to dbfilename

        try{
            if(getFlag()>=0) {
                context = ctx;
                Intent trepnProfiler = new Intent();
                trepnProfiler.setClassName("com.quicinc.trepn", "com.quicinc.trepn.TrepnService");
                ctx.startService(trepnProfiler);
                File sdCard = Environment.getExternalStorageDirectory();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                loadPreferences(ctx, sdCard.getAbsolutePath() + "/trepn/saved_preferences/trepnPreferences/All.pref");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Intent startWithDb = new Intent("com.quicinc.trepn.start_profiling");
                startWithDb.putExtra("com.quicinc.trepn.database_file", "greendroid2");
                ctx.sendBroadcast(startWithDb);
                try {
                    Thread.sleep(warmUpTimeMilis);
                } catch (InterruptedException e) {
                }
            }
        }
        catch( Exception e){
            context=null;
        }
    }


    /*
    * For test oriented monitoring
    */
    public static void startProfilingTest(Context ctx){ // potentially can receive a String to dbfilename

       try{
           File sdCard = Environment.getExternalStorageDirectory();
           File ft = new File(sdCard.getAbsolutePath() + "/trepn/TracedTests/TracedTests.txt");
           if (!ft.exists()) {
               try {
                   ft.createNewFile();
               } catch (IOException e) {
               }
           }
           int flg = getFlag();
           if(flg==0) {
               Intent trepnProfiler = new Intent();
               trepnProfiler.setClassName("com.quicinc.trepn", "com.quicinc.trepn.TrepnService");
               ctx.startService(trepnProfiler);
               try {
                   Thread.sleep(1500);
               } catch (InterruptedException e) {
               }
               loadPreferences(ctx, sdCard.getAbsolutePath() + "/trepn/saved_preferences/trepnPreferences/All.pref");
               try {
                   Thread.sleep(1500);
               } catch (InterruptedException e) {
               }
               Intent startWithDb = new Intent("com.quicinc.trepn.start_profiling");
               startWithDb.putExtra("com.quicinc.trepn.database_file", "greendroid2Trace");
               ctx.sendBroadcast(startWithDb);
               try {
                   Thread.sleep(warmUpTimeMilis*3);
               } catch (InterruptedException e) {
               }
               File directory2 = new File(sdCard.getAbsolutePath() + "/trepn/Traces");
               File directory3 = new File(sdCard.getAbsolutePath() + "/trepn/Measures");
               File file = new File(sdCard.getAbsolutePath() + "/trepn/"+ "TracedMethods.txt");
               if (!file.exists()) {
                   try {
                       file.createNewFile();
                   } catch (IOException e) {
                   }
               }
               updateState(ctx, 1, "started");
           }
           else if(flg==1){ // just measure
               File directory3 = new File(sdCard.getAbsolutePath() + "/trepn/Measures");
               qtdMeasures = directory3.list().length;
               Intent trepnProfiler = new Intent();
               trepnProfiler.setClassName("com.quicinc.trepn", "com.quicinc.trepn.TrepnService");
               ctx.startService(trepnProfiler);
               try {
                   Thread.sleep(1500);
               } catch (InterruptedException e) {
               }
               loadPreferences(ctx, sdCard.getAbsolutePath() + "/trepn/saved_preferences/trepnPreferences/All.pref");
               try {
                   Thread.sleep(1500);
               } catch (InterruptedException e) {
               }
               Intent startWithDb = new Intent("com.quicinc.trepn.start_profiling");
               startWithDb.putExtra("com.quicinc.trepn.database_file", "greendroid2Trace");
               ctx.sendBroadcast(startWithDb);
               try {
                   Thread.sleep(warmUpTimeMilis*3);
               } catch (InterruptedException e) {
               }
               updateState(ctx, 1, "started");

           }
           else { // just tracing
               File directory2 = new File(sdCard.getAbsolutePath() + "/trepn/Traces");
               File file = new File(sdCard.getAbsolutePath() + "/trepn/"+ "TracedMethods.txt");
               if (!file.exists()) {
                   try {
                       boolean b =  file.createNewFile();
                   } catch (IOException e) {
                   }
               }
               qtdTraces = directory2.list().length;
           }
       }
       catch (Exception e){
           context = null;
       }

    }

    /*
    * For test oriented monitoring
    * */
    public static void stopProfilingTest(Context ctx){

        try{
            int flg = getFlag();
            if(flg==0) {
                updateState(ctx, 0, "stopped");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Intent stopProfiling = new Intent("com.quicinc.trepn.stop_profiling");
                ctx.sendBroadcast(stopProfiling);
                Intent intent = new Intent("com.quicinc.trepn.export_to_csv");
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/trepn/Traces");
                intent.putExtra("com.quicinc.trepn.export_db_input_file", "greendroid2Trace");
                File directory3 = new File(sdCard.getAbsolutePath() + "/trepn/Measures");
                int x = directory3.list().length;
                intent.putExtra("com.quicinc.trepn.export_csv_output_file","GreendroidResultTrace" + x);
                ctx.sendBroadcast(intent);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {

                }
                File f1 = new File(sdCard.getAbsolutePath() + "/trepn/TracedMethods.txt");
                if (!f1.exists()){
                    try {
                        f1.createNewFile();
                    } catch (IOException e) {
                    }
                }
                File f2 = new File(sdCard.getAbsolutePath() + "/trepn/Traces/TracedMethods" + dir.list().length + ".txt");
                File f4= new File(sdCard.getAbsolutePath() + "/trepn/Measures/" + "GreendroidResultTrace" + x + ".csv");
                File f3 = new File(sdCard.getAbsolutePath() + "/trepn/"+ "GreendroidResultTrace" + x + ".csv");

                try {
                    copyFile(f1, f2);
                    copyFile(f3,f4);
                } catch (IOException e) {
                    try {
                        f1.createNewFile();
                        copyFile(f1, f2);
                        copyFile(f3,f4);
                    } catch (IOException e1) {
                    }
                }
                f1.delete();
                f3.delete();
                if(directory3.list().length>0 && directory3.list().length-1!=qtdMeasures){
                    File f = new File(sdCard.getAbsolutePath() + "/trepn/Measures/"+"GreendroidResultTrace" + directory3.list().length +".csv");
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                    }
                }
            }
            else if(flg==1) { // just measure energy comsuption
                File sdCard = Environment.getExternalStorageDirectory();
                File directory3 = new File(sdCard.getAbsolutePath() + "/trepn/Measures");
                updateState(ctx, 0, "stopped");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Intent stopProfiling = new Intent("com.quicinc.trepn.stop_profiling");
                ctx.sendBroadcast(stopProfiling);
                Intent intent = new Intent("com.quicinc.trepn.export_to_csv");
                //File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/trepn/Traces");
                intent.putExtra("com.quicinc.trepn.export_db_input_file", "greendroid2Trace");
                int x = directory3.list().length;
                intent.putExtra("com.quicinc.trepn.export_csv_output_file","GreendroidResultTrace" + x);
                //intent.putExtra("com.quicinc.trepn.export_csv_output_file", "Measures/GreendroidResultTrace" + directory3.list().length);

                ctx.sendBroadcast(intent);
                try {
                    Thread.sleep(coolDownTimeMilis);
                } catch (InterruptedException e) {
                }
                File f4= new File(sdCard.getAbsolutePath() + "/trepn/Measures/" + "GreendroidResultTrace" + x + ".csv");
                File f3 = new File(sdCard.getAbsolutePath() + "/trepn/"+ "GreendroidResultTrace" + x + ".csv");
                try {
                    copyFile(f3,f4);
                } catch (IOException e) {
                }
                f3.delete();
                if(directory3.list().length-1!=qtdMeasures){
                    File f = new File(sdCard.getAbsolutePath() + "/trepn/Measures/"+"GreendroidResultTrace" + directory3.list().length + ".csv");
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                    }
                }
            }
            else { // just trace
                File sdCard = Environment.getExternalStorageDirectory();
                File f1 = new File(sdCard.getAbsolutePath() + "/trepn/TracedMethods.txt");
                File dir = new File(sdCard.getAbsolutePath() + "/trepn/Traces");
                File f2 = new File(sdCard.getAbsolutePath() + "/trepn/Traces/TracedMethods" + dir.list().length + ".txt");
                try {
                    copyFile(f1, f2);
                }
                catch (IOException e) {
                    try {
                        f1.createNewFile();
                        copyFile(f1, f2);
                    }
                    catch (IOException e1) {
                    }
                }
                f1.delete();
                File directory2 = new File(sdCard.getAbsolutePath() + "/trepn/Traces");
                if(directory2.list().length>0 && directory2.list().length-1!=qtdTraces){
                    File f = new File(sdCard.getAbsolutePath() + "/trepn/Traces/" + "TracedMethods" + directory2.list().length + ".txt");
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                    }
                }
            }
            context = null;
        }
        catch (Exception e){
            context = null;
        }


    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }

    /*
    * For method oriented monitoring
    */
    public static void stopProfiling(Context ctx){
        try{
            if(getFlag()>=0) {
                try {
                    Thread.sleep(500*2);
                } catch (InterruptedException e) {
                }
                Intent stateUpdate = new Intent("com.quicinc.Trepn.UpdateAppState");
                stateUpdate.putExtra("com.quicinc.Trepn.UpdateAppState.Value", String.valueOf(-1));
                stateUpdate.putExtra("com.quicinc.Trepn.UpdateAppState.Value.Desc", "");
                ctx.sendBroadcast(stateUpdate);
                Intent stopProfiling = new Intent("com.quicinc.trepn.stop_profiling");
                ctx.sendBroadcast(stopProfiling);
                Intent intent = new Intent("com.quicinc.trepn.export_to_csv");
                intent.putExtra("com.quicinc.trepn.export_db_input_file", "greendroid2");
                File sdCard = Environment.getExternalStorageDirectory();
                File directory = new File(sdCard.getAbsolutePath() + "/trepn");
                intent.putExtra("com.quicinc.trepn.export_csv_output_file", "GrendroidResult" + directory.list().length);
                ctx.sendBroadcast(intent);
                try {
                    Thread.sleep(coolDownTimeMilis);
                } catch (InterruptedException e) {
                }
            }
            context = null;
        }
        catch (Exception e){
            context = null;
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
        } finally {
            try {
                if (sourceChannel != null)
                    sourceChannel.close();
                if (destChannel != null)
                    destChannel.close();
            } catch (IOException e) {

            }finally {
                close(destChannel);
                close(sourceChannel);
            }

        }
    }

    private static int getFlag(){
        Scanner sc = null;
        File sdCard = Environment.getExternalStorageDirectory();
        File flag = new File(sdCard.getAbsolutePath() + "/trepn/GDflag");
        try {
            sc = new Scanner(flag);
        } catch (FileNotFoundException e) {
            return 0;
        }
        return sc.nextInt();

    }

    public static void traceMethod(String methodName) {
        if(getFlag()<=0) { // se so faz trace ou tudo
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/trepn");
            File file = new File(directory, "TracedMethods.txt");
            FileOutputStream fOut = null;
            OutputStreamWriter osw = null;
            try {
                fOut = new FileOutputStream(file, true);
                osw = new OutputStreamWriter(fOut);
                osw.write(methodName + "\n");
                osw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(osw);
                close(fOut);
            }
        }
    }

    public static void updateState(Context ctx, int state, String description) {
        if (getFlag() >= 0) {
            ctx = ctx!=null ? ctx : (context!=null? context : initContext() );
            if (state == 0) {
                Intent stateUpdate = new Intent("com.quicinc.Trepn.UpdateAppState");
                globalState--;
                int x = Integer.valueOf(globalState);
                stateUpdate.putExtra("com.quicinc.Trepn.UpdateAppState.Value", String.valueOf(x));
                stateUpdate.putExtra("com.quicinc.Trepn.UpdateAppState.Value.Desc", description);
                ctx.sendBroadcast(stateUpdate);
            } else {
                Intent stateUpdate = new Intent("com.quicinc.Trepn.UpdateAppState");
                ++globalState;
                int x = Integer.valueOf(globalState);
                stateUpdate.putExtra("com.quicinc.Trepn.UpdateAppState.Value",String.valueOf(x));
                stateUpdate.putExtra("com.quicinc.Trepn.UpdateAppState.Value.Desc", description);
                ctx.sendBroadcast(stateUpdate);
            }
        }
    }

    public static void traceTest(String testName){
        if(getFlag()>0) {
            File sdCard = Environment.getExternalStorageDirectory();
            File directory = new File(sdCard.getAbsolutePath() + "/trepn/TracedTests");
            File file = new File(directory, "TracedTests.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                }
            }
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(file, true);
            } catch (FileNotFoundException e) {
            }
            finally {
                close(fOut);
            }
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            try {
                osw.write(testName + "\n");
                osw.flush();
                osw.close();
            } catch (IOException e) {
            }
            finally {
                close(osw);
            }
        }
    }

    private static Application getApplicationUsingReflection() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null, (Object[]) null);
    }



    public static void close(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {
            //log the exception
        }
    }
}
