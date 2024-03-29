package com.voicerec;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class VoiceRecActivity extends Activity 
{
    /** Called when the activity is first created. */
	  private Button myButton1;
	  private Button myButton2;
	  private Button myButton3;
	  private Button myButton4;
	  
      private static final int MSG_SET_PLAYSTATUS = 1;  
	  private static final int MSG_SET_PLAYSTATUS2 = 2;
	  

	  private String strTempFile = "VoiceRec";
	  private String TAG = "VoiceRec";
	  
	  private File myRecAudioFile;
	  private File myRecOutPutAudioFile;
	  private File myRecAudioDir;
	  private File myPlayFile;
	  private MediaRecorder mMediaRecorder01;

	  private ArrayList<String> recordFiles;
	  private ArrayAdapter<String> adapter;
	  private TextView myTextView1;
	  private boolean sdCardExit;
	  private boolean isStopRecord;
	  
	  private static final int MENU_EXIT = Menu.FIRST;
	  private AlertDialog.Builder builder;
	  AudioRecord audioRecord;
	  private ProgressDialog myDialog;
	  RecordAudio recordTask;
	  PlayAudio playTask;

	  int play_file = 0;
	  boolean isRecording = false,isPlaying = false;
	  
	  int frequency = 8000; //8k
	  int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	  int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	  
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
        myButton1 = (Button) findViewById(R.id.ImageButton01);
        myButton2 = (Button) findViewById(R.id.ImageButton02);
        myButton3 = (Button) findViewById(R.id.ImageButton03);
        myButton4 = (Button) findViewById(R.id.ImageButton04);
        
        myTextView1 = (TextView) findViewById(R.id.TextView01);

        myButton2.setEnabled(false);
        myButton3.setEnabled(false);

    	myRecAudioFile = null;
	    myRecOutPutAudioFile = null;
	    
        sdCardExit = Environment.getExternalStorageState().equals(
            android.os.Environment.MEDIA_MOUNTED);

        if (sdCardExit)
        {
          myRecAudioDir = Environment.getExternalStorageDirectory();
        }
        
       //myRecAudioFile = new File(
    	      //  Environment.getExternalStorageDirectory() + "/2.pcm");

        //getRecordFiles();

        myButton1.setOnClickListener(new Button.OnClickListener()
        {
          @Override
          public void onClick(View arg0)
          {
        	  Log.i(TAG, "click Rec start.....");
            if (!sdCardExit)
              {
                Toast.makeText(VoiceRecActivity.this, "insert SD Card",
                    Toast.LENGTH_LONG).show();
                return;
              }
              
              Log.i(TAG, "Rec start.....");

              if (myRecAudioFile != null && myRecOutPutAudioFile != null)
              {
            	  myRecAudioFile.setWritable(true);
            	  myRecAudioFile.delete();
            	  myRecOutPutAudioFile.setWritable(true);
            	  myRecOutPutAudioFile.delete();
            	  myRecAudioFile = null;
            	  myRecOutPutAudioFile = null;
              }
              
               File path = new File(
            	        Environment.getExternalStorageDirectory() + "/");
               path.mkdirs();
               try {
            	    	myRecAudioFile = File.createTempFile("recording", ".pcm", path);
               } catch (IOException e) {
            	      throw new RuntimeException("Couldn't create file on SD card", e);
               }              
               
              record();
              
              myTextView1.setText("record....");
              
              myButton1.setEnabled(false);
              myButton2.setEnabled(true);
              myButton3.setEnabled(false);
              myButton4.setEnabled(false);
              isStopRecord = false;
              
              Log.i(TAG, "finish.....");
          }
        });
 
        myButton2.setOnClickListener(new Button.OnClickListener()
        {
          @Override
          public void onClick(View arg0)
          {
            // TODO Auto-generated method stub
        	  stopRecording();
              //adapter.add(myRecAudioFile.getName());
              myTextView1.setText("stop�G" + myRecAudioFile.getName());
              myButton2.setEnabled(false);
              myButton3.setEnabled(true);
              myButton1.setEnabled(true);              
              myButton4.setEnabled(true);     
              isStopRecord = true;
            
          }
        });
        
        myButton3.setOnClickListener(new Button.OnClickListener()
        {
          public void onClick(View arg0)
          {
        	  if (myButton3.getText().toString().equals("Play"))
        	  {
        	    if (myRecOutPutAudioFile == null)
        	    {
                    //Progress
                    myDialog = ProgressDialog.show
                    (
                        VoiceRecActivity.this,
                        "reduce noise",
                        "please wait...",
                        true
                    );
                    
                    new Thread()
                    {
                      public void run()
                      {
                          File path = new File(
                        	        Environment.getExternalStorageDirectory() + "/");
                           path.mkdirs();
                           try {
                        	    	myRecOutPutAudioFile = File.createTempFile("recording", ".pcm", path);
                           } catch (IOException e) {
                        	      throw new RuntimeException("Couldn't create file on SD card", e);
                           }              

                        DataInputStream dis;
                  		DataOutputStream dos;

          				try {
          					dis = new DataInputStream(new FileInputStream(myRecAudioFile));
          					dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(myRecOutPutAudioFile)));
          					
          	 	            VoiceProccessing vp = new VoiceProccessing();
          		            int bufferSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
          		            short[] audiodata = new short[VoiceProccessing.FFT_SIZE];
          		            short[] maudiodata = new short[VoiceProccessing.FFT_SIZE2];
          	                try {
          	                	int flag=0;
          						while (dis.available() > 0) 
          						{
          						      int i = 0;
          						      if (flag == 1)
          						      {
          						        while (dis.available() > 0 && i < audiodata.length) 
          						        {
          						          audiodata[i] = dis.readShort();
          						          i++;
          						        }
          						        vp.proccess_running(audiodata, audiodata.length, dos);
          						      }
          						      else
          						      {
          						    	  flag = 0;
            						      while (dis.available() > 0 && i < maudiodata.length) {
            						        	maudiodata[i] = dis.readShort();
                						        i++;
                						  }
                						  vp.proccess_running(maudiodata, maudiodata.length, dos);
          						      }
          						}
          						dis.close();  
          						dos.close();  
          					} catch (IOException e) {
          						// TODO Auto-generated catch block
          						e.printStackTrace();
          					}
          					
          				} catch (FileNotFoundException e) {
          					// TODO Auto-generated catch block
          					e.printStackTrace();
          				}  
           
                  	    play_file = 1;
                  		play(); 
                   	  
                        myDialog.dismiss();
                      }
                    }.start();
        	    }
        	    
          	    play_file = 1;
          		play();        	    
        		myButton3.setText("Stop");
        	  }
        	  else
        	  {
        		  myButton3.setText("Play");
        		  stopPlaying();
        	  }
          }
        });

        myButton4.setOnClickListener(new Button.OnClickListener()
        {
          public void onClick(View arg0)
          {
        	  if (myButton3.getText().toString().equals("Play"))
        	  {
        		play_file = 2;
        		play(); 
        		myButton4.setText("Stop");
        	  }
        	  else
        	  {
        		  myButton4.setText("Play");
        		  stopPlaying();
        	  }
          }
        });        
        
    }

	   public boolean onKeyDown(int keyCode, KeyEvent event)
	    {
		    String url = "";
		
		    if (keyCode == KeyEvent.KEYCODE_BACK)
		    {
		    		builder = new AlertDialog.Builder(this);
	                builder.setMessage("Are you exit?");
	                builder.setCancelable(false);
		               
	                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                     public void onClick(DialogInterface dialog, int id)
		                    {
	       	                 android.os.Process.killProcess(android.os.Process.myPid());           

		                     finish();
		                    }
		                });
		               
	                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	                     public void onClick(DialogInterface dialog, int id)
		                    {
		                    }
		                });
		                
		              AlertDialog alert = builder.create();
		              alert.show();
		              
		              return false;
		    }
		    else if (keyCode == KeyEvent.KEYCODE_MENU)
		    {
		    	return super.onKeyDown(keyCode, event);
		    }
		
		    return super.onKeyDown(keyCode, event);
	     
		    
	}	
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    super.onCreateOptionsMenu(menu);


	    menu.add(0 , MENU_EXIT, 1 , "Exit")
	    .setAlphabeticShortcut('E');
	    
	    return true;  
	 }
	public boolean onOptionsItemSelected(MenuItem item)
	{
		    switch (item.getItemId())
		    { 		      

	          case MENU_EXIT:
	              android.os.Process.killProcess(android.os.Process.myPid());        
	              recordTask.cancel(true);
	        	  finish();
	        	  break ;
		    }
		      return true ;
	}	
	
	  public void play() {
		    //startPlaybackButton.setEnabled(true);

		    playTask = new PlayAudio();
		    playTask.execute();

		    //stopPlaybackButton.setEnabled(true);
		  }

		  public void stopPlaying() {
		    isPlaying = false;
		    //stopPlaybackButton.setEnabled(false);
		    //startPlaybackButton.setEnabled(true);
		  }

		  public void record() {
		    recordTask = new RecordAudio();
		    recordTask.execute();
		  }
		  
		  public void stopRecording() {
			if (audioRecord != null)
	        {
	        		audioRecord.release();
	        }
		    isRecording = false;
		  }
	
    
	    private class RecordAudio extends AsyncTask<Void, Integer, Void> {
	        @Override
	        protected Void doInBackground(Void... params) 
	        {
	          isRecording = true;
	          //FileOutputStream  dos;
	          OutputStream os = null;
			try {
				os = new FileOutputStream(myRecAudioFile);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	          BufferedOutputStream bos = new BufferedOutputStream(os);
	          DataOutputStream dos = new DataOutputStream(bos);
	          
	          try {
	        	  
	        	if (audioRecord != null)
	        	{
	        		audioRecord.release();
	        	}
	        	
	        	Log.i(TAG, "start rec...");
	        	
	            //dos = new FileOutputStream(myRecAudioFile);
	            
	            int bufferSize = AudioRecord.getMinBufferSize(frequency,
	                channelConfiguration, audioEncoding);
	            
	            Log.i(TAG, "start rec1...");
	            
	            audioRecord = new AudioRecord(
	                MediaRecorder.AudioSource.MIC, frequency,
	                channelConfiguration, audioEncoding, bufferSize);
	            Log.i(TAG, "start rec2...");
	            
	            short [] buffer = new short [bufferSize];
	            audioRecord.startRecording();
	            int r = 0;
	            Log.i(TAG, "start rec4...bufferSize" + bufferSize);
	            
	            while (isRecording) 
	            {
	              int bufferReadResult = audioRecord.read(buffer, 0,
	                  bufferSize);

	              if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) { 
						try {
			                for (int i = 0; i < bufferReadResult; i++) 
			                {
					            //short d = buffer[i];
					            //buffer[i] = (short) ( ((d & 0xff)<<8)  | ((d & 0xff00)>>8)) ;
			                    dos.writeShort(buffer[i]);
					               //Log.i(TAG, "input: " + Short.toString(buffer[i]));
			                }
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
     	              }
	              //Log.i(TAG, "proccess_running finish...");

	              publishProgress(new Integer(r));
	              r++;
	            }
	            dos.close();
	            
	            if (audioRecord != null)
	              audioRecord.stop();
	            
	          } catch (Exception e) {
	        	  e.printStackTrace();
	          }
	          return null;
	        }
	    }

	    private class PlayAudio extends AsyncTask<Void, Integer, Void> {
	            @Override
	            protected Void doInBackground(Void... params) 
	            {
	              isPlaying = true;

	              int bufferSize = AudioTrack.getMinBufferSize(frequency,channelConfiguration, audioEncoding);
	              short[] audiodata = new short[bufferSize / 4];

	              try {
	            	DataInputStream dis = null;
	            	if (play_file == 1)
	            		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(myRecOutPutAudioFile)));
	            	else
	            		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(myRecAudioFile)));
	            	
	                AudioTrack audioTrack = new AudioTrack(
	                    AudioManager.STREAM_MUSIC, frequency,
	                    channelConfiguration, audioEncoding, bufferSize,
	                    AudioTrack.MODE_STREAM);

	                audioTrack.play();
	                while (isPlaying && dis.available() > 0) {
	                  int i = 0;
	                  while (dis.available() > 0 && i < audiodata.length) {
	                    audiodata[i] = dis.readShort();
	                    i++;
	                  }
	                  audioTrack.write(audiodata, 0, audiodata.length);
	                }
	                dis.close();
	                
					Message cmsg = new Message();
	            	if (play_file == 1)
	            	{
						cmsg.what = MSG_SET_PLAYSTATUS;
						myHandler.sendMessage(cmsg);
	            	}
	            	else
	            	{
						cmsg.what = MSG_SET_PLAYSTATUS2;
						myHandler.sendMessage(cmsg);
	            	}
	                
	              } catch (Exception e) {
		        	  e.printStackTrace();
		          }
	              return null;
	            }
	    }	       
	
		  public Handler myHandler = new Handler(){
			    public void handleMessage(Message msg) {
			        switch(msg.what)
			        {
			          case MSG_SET_PLAYSTATUS:
		        		  myButton3.setText("Play");
		                  break;
			          case MSG_SET_PLAYSTATUS2:
		        		  myButton4.setText("Play");
			              break;
			        }
			        
			        super.handleMessage(msg);
			    }
			};  
	  

}