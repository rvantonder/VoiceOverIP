package cs.sun.ac.za.voip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class VoipClient {

	  private InetAddress ip; //the ip address to which the client wishes to send
	  private int dest_port; //the port to which the client wishes to send
	  private int src_port; //the clients own port, to listen on for traffic
	  private DatagramSocket receive_socket; //the socket to receive on
	  private DatagramSocket send_socket; //the socket to send on
	  
	  boolean stopCapture = false;
	  //ByteArrayOutputStream byteArrayOutputStream;
	  AudioFormat audioFormat;
	  TargetDataLine targetDataLine;
	  AudioInputStream audioInputStream;
	  SourceDataLine sourceDataLine;
	  
	  public VoipClient(String ip, int dest_port, int src_port)	{
		  try	{
			  this.ip = InetAddress.getByName(ip);
		  } catch (Exception e)	{
			  System.out.println(e.getMessage());
		  }
		  
		  this.src_port = src_port;
		  this.dest_port = dest_port;
		  
		  try {
			  this.receive_socket = new DatagramSocket(src_port);
			  this.send_socket = new DatagramSocket();
		  } catch (Exception e)	{
			  System.out.println(e.getMessage());
		  }
		  
		  Thread CaptureAudio = new Thread(new CaptureAudio(this.send_socket, this.ip, this.dest_port));
		  CaptureAudio.start();
		  
		  Thread PlayAudio = new Thread(new PlayAudio(this.receive_socket, this.src_port));
		  PlayAudio.start();
		  
	  }
	  
	  //This method creates and returns an
	  // AudioFormat object for a given set
	  // of format parameters.  If these
	  // parameters don't work well for
	  // you, try some of the other
	  // allowable parameter values, which
	  // are shown in comments following
	  // the declarations.
	  private AudioFormat getAudioFormat(){
	    float sampleRate = 8000.0F;
	    //8000,11025,16000,22050,44100
	    int sampleSizeInBits = 16;
	    //8,16
	    int channels = 1;
	    //1,2
	    boolean signed = true;
	    //true,false
	    boolean bigEndian = false;
	    //true,false
	    return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	  }//end getAudioFormat
	  
	  //This method captures audio input
	  // from a microphone and saves it in
	  // a ByteArrayOutputStream object.
	  class CaptureAudio implements Runnable	{
		  
		  private byte[] tempBuffer;
		  private DatagramSocket socket;
		  private InetAddress ip;
		  private int port;
		
		  public CaptureAudio(DatagramSocket socket, InetAddress ip, int port)	{
			  this.socket = socket;
			  this.ip = ip;
			  this.port = port;
			  this.tempBuffer = new byte[10000];
		  }
		  
		  public void run()	{  
			  try {
				  //Get everything set up for capture
				  audioFormat = getAudioFormat();
				  DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
				  targetDataLine = (TargetDataLine)
	      		  AudioSystem.getLine(dataLineInfo);
				  targetDataLine.open(audioFormat);
				  targetDataLine.start();

				//Create a thread to capture the
				// microphone data and start it
				// running.  It will run until
				// the Stop button is clicked.
				
				//Thread captureThread = new Thread(new CaptureThread(send_socket, ip, dest_port));
				//captureThread.start();
				
				//capture the audio now
				  //byteArrayOutputStream = new ByteArrayOutputStream();
				  stopCapture = false;
				  try{//Loop until stopCapture is set
			        // by another thread that
			        // services the Stop button.
					  while(!stopCapture){
			        //Read data from the internal
			        // buffer of the data line.
						  int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
						  if(cnt > 0){
			          //Save data in output stream
			          // object.
			          //byteArrayOutputStream.write(tempBuffer, 0, cnt); //XXX save to memory, put socket here?
							  DatagramPacket outPacket = new DatagramPacket(tempBuffer, tempBuffer.length, this.ip, this.port);
							  this.socket.send(outPacket);
						  }//end if
					  }//end while
					  //byteArrayOutputStream.close();
				  }catch (Exception e) {
					  System.out.println(e);
					  System.exit(0);
				  }//end catch
				
			  } catch (Exception e) {
				  System.out.println(e);
	      		  System.exit(0);
			  }//end catch
		}
	}//end captureAudio method
	  
	  class PlayAudio implements Runnable {
		  
		  private DatagramSocket socket;
		  private int port;
		  private byte[] tempBuffer;
		  
		  public PlayAudio(DatagramSocket socket, int port)	{
			  this.socket = socket;
			  this.port = port;
			  this.tempBuffer = new byte[10000];
		  }
		  
		  public void run()	{
		    try{
		      //Get everything set up for
		      // playback.
		      //Get the previously-saved data
		      // into a byte array object.
		    	
		      //byte audioData[] = byteArrayOutputStream.toByteArray(); //XXX saved stuff, put socket here

			 DatagramPacket inPacket;
		    	
		      while (true)	{
		      
		      inPacket = new DatagramPacket(tempBuffer, tempBuffer.length);
			  this.socket.receive(inPacket);
			  
			  byte[] audioData = inPacket.getData();
		    	  
		      //Get an input stream on the
		      // byte array containing the data
		      InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
		      AudioFormat audioFormat = getAudioFormat();
		      audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length/audioFormat.getFrameSize());
		      DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		      sourceDataLine = (SourceDataLine)
		      AudioSystem.getLine(dataLineInfo);
		      sourceDataLine.open(audioFormat);
		      sourceDataLine.start();

		      //Create a thread to play back
		      // the data and start it
		      // running.  It will run until
		      // all the data has been played
		      // back.
		      
		      //Thread playThread = new Thread(new PlayThread());
		      //playThread.start();
		     try { 
		      int cnt;
		      //Keep looping until the input
		      // read method returns -1 for
		      // empty stream.
		      while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
		        if(cnt > 0){
		          //Write data to the internal
		          // buffer of the data line
		          // where it will be delivered
		          // to the speaker.
		          sourceDataLine.write(tempBuffer, 0, cnt);
		        }//end if
		      }//end while
		      //Block and wait for internal
		      // buffer of the data line to
		      // empty.
		      sourceDataLine.drain();
		      sourceDataLine.close();
		    }catch (Exception e) {
		      System.out.println(e);
		      System.exit(0);
		    }//end catch
			} //while
		    } catch (Exception e) {
		      System.out.println(e);
		      System.exit(0);
		    }//end catch
		  }

		}//end playAudio
	  
	  class PlayThread extends Thread{
		  byte tempBuffer[] = new byte[10000];

		  public void run(){
		    try{
		      int cnt;
		      //Keep looping until the input
		      // read method returns -1 for
		      // empty stream.
		      while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
		        if(cnt > 0){
		          //Write data to the internal
		          // buffer of the data line
		          // where it will be delivered
		          // to the speaker.
		          sourceDataLine.write(tempBuffer, 0, cnt);
		        }//end if
		      }//end while
		      //Block and wait for internal
		      // buffer of the data line to
		      // empty.
		      sourceDataLine.drain();
		      sourceDataLine.close();
		    }catch (Exception e) {
		      System.out.println(e);
		      System.exit(0);
		    }//end catch
		  }//end run
		}//end inner class PlayThread
	  /*
	  private void send_msg()	{
		  String message = "Hello";
		  byte[] tempBuffer = message.getBytes();
		  DatagramPacket outPacket = new DatagramPacket(tempBuffer, tempBuffer.length, ip, this.dest_port);
		  try {
			  socket.send(outPacket);
		  } catch (Exception e) {
			  System.out.println(e.getMessage());
		  }
	  }
	  
	  private void receive()	{
		  byte tempBuffer[] = new byte[1024];
		  DatagramPacket inPacket = new DatagramPacket(tempBuffer, tempBuffer.length);
		  try	{
			  socket.receive(inPacket);
		  } catch (Exception e)	{
			  System.out.println(e.getMessage());
		  }
		  String message = new String(inPacket.getData());
		  System.out.println(message);
	  }
	  */
	  public static void main(String args[]){
		  String ip = args[0]; //destination ip
		  int dest_port = Integer.parseInt(args[1]); //destination port
		  int src_port = Integer.parseInt(args[2]); //source port
		  
		  VoipClient client = new VoipClient(ip, dest_port, src_port);		  

	  }
}

