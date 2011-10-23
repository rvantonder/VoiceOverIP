//package cs.sun.ac.za.voip;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class VoipClient implements ActionListener{

	//gui
	private static JFrame frame;
	private static JTextField textField;
	private static JTextArea textArea;
	private static JScrollPane scrollPane;
	private static JList userList;

	private InetAddress client_ip; //the ip address the client wishes to call
	private InetAddress server_ip; //the ip of the server
	private int receive_port = 3001; //the port to which the client wishes to send
	private int send_port = 3002; //the clients own port, to listen on for traffic
	private int server_port; //the port with which to connect to the server
	private DatagramSocket receive_socket; //the socket to receive on
	private DatagramSocket send_socket; //the socket to send on
	private Socket server_socket; //TCP socket to server

	private OutputStream out = null;
	private BufferedReader in = null;

	private static boolean connected = false;
	private static boolean stopCapture;
	private static boolean stopPlay;

	AudioFormat audioFormat;
	TargetDataLine targetDataLine;
	AudioInputStream audioInputStream;
	SourceDataLine sourceDataLine;

	public VoipClient(){

		//build the gui
		frame = new JFrame("VOIP");

		JPanel pane = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		GridBagConstraints c2 = new GridBagConstraints();
		GridBagConstraints c3 = new GridBagConstraints();

		textArea = new JTextArea("Commands: \n \\call <ip> \n \\dc \n \\msg <text message>\n");
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		textField = new JTextField();
		textField.addActionListener(this);
		textField.setText("\\call 127.0.0.1");
		userList = new JList();

		c1.fill = GridBagConstraints.BOTH;
		c1.insets = new Insets(10, 10, 5, 5); //top, left, bottom, right
		c1.gridwidth = 1;
		c2.gridheight = 1;
		c1.ipadx = 350;
		c1.ipady = 400; 
		c1.weightx = 1;
		c1.weighty = 1;
		c1.gridx = 0;
		c1.gridy = 0;
		pane.add(scrollPane, c1);

		c2.fill = GridBagConstraints.BOTH;
		c2.insets = new Insets(5, 10, 10, 10);
		c2.gridwidth = 1;
		c2.gridheight = 1;
		c2.ipadx = 350;
		c2.ipady = 70;
		c2.weightx = 0.3;
		c2.weighty = 0.3;
		c2.gridx = 0;
		c2.gridy = 1;
		pane.add(textField, c2);

		c3.fill = GridBagConstraints.BOTH;
		c3.insets = new Insets(10, 5, 10, 10);
		c3.gridwidth = 1;
		c3.gridheight = 1;
		c3.ipadx = 150;
		c3.ipady = 400;
		c3.weightx = 0.2;
		c3.weighty = 0.2;
		c3.gridx = 1;
		c3.gridy = 0;
		pane.add(userList, c3);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(pane);
		frame.pack();
		/*
		 * requestFocusInWindow must be called after component realization,
		 * frame.pack() and before the frame is displayed.
		 */
		textField.requestFocusInWindow();
		textField.setSelectionStart(6);
		textField.setSelectionEnd(15);
		frame.setSize(500, 500);
		frame.setVisible(true); 

		stopCapture = false;
		stopPlay = false;
	}

	/*
	 * This method creates and returns an AudioFormat object for a given set
	 * of format parameters.
	 */
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
	}

	/* 
	 * This method captures audio input
	 * from a microphone and sends it as a UDP packet.
	 */
	class CaptureAudio implements Runnable	{

		private byte[] tempBuffer;
		private DatagramSocket socket;
		private InetAddress ip;
		private int port;

		public CaptureAudio(DatagramSocket socket, InetAddress ip, int port)	{
			this.socket = socket;
			this.ip = ip;
			this.port = port;
			this.tempBuffer = new byte[4000];
		}

		public void run()	{  
			try {

				stopCapture = false;

				//Get everything set up for capture
				audioFormat = getAudioFormat();
				DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
				targetDataLine = (TargetDataLine)
				AudioSystem.getLine(dataLineInfo);
				targetDataLine.open(audioFormat);
				targetDataLine.start();

				try{
					//Loop until stopCapture is set by another thread.
					while(!stopCapture){
						//Read data from the internal buffer of the data line.
						int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
						if(cnt > 0){
							DatagramPacket outPacket = new DatagramPacket(tempBuffer, tempBuffer.length, this.ip, this.port);
							this.socket.send(outPacket);
						}
					}
				}catch (Exception e) {}

			} catch (Exception e) {}
		}
	}

	/* This method receives audio input
	 * from a UDP packet and plays it.
	 */
	class PlayAudio implements Runnable {

		private DatagramSocket socket;
		private byte[] tempBuffer;

		public PlayAudio(DatagramSocket socket)	{
			this.socket = socket;
			this.tempBuffer = new byte[4000];
		}

		public void run()	{
			try{

				DatagramPacket inPacket;
				stopPlay = false;

				//Loop until stopPlay is set by another thread.
				while (!stopPlay)	{

					//Put received data into a byte array object
					inPacket = new DatagramPacket(tempBuffer, tempBuffer.length);
					this.socket.receive(inPacket);

					byte[] audioData = inPacket.getData();

					//Get an input stream on the byte array containing the data
					InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
					AudioFormat audioFormat = getAudioFormat();
					audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length/audioFormat.getFrameSize());
					DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
					sourceDataLine = (SourceDataLine)
					AudioSystem.getLine(dataLineInfo);
					sourceDataLine.open(audioFormat);
					sourceDataLine.start();

					try { 
						int cnt;
						//Keep looping until the input read method returns -1 for empty stream.
						while((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1){
							if(cnt > 0){
								//Write data to the internal buffer of the data line where it will be delivered to the speaker.
								sourceDataLine.write(tempBuffer, 0, cnt);
							}
						}
						//Block and wait for internal buffer of the data line to empty.
						sourceDataLine.drain();
						sourceDataLine.close();
					}catch (Exception e) {}
				}
			} catch (Exception e) {}
		}

	}

	/*
	 * Initiates a call between two clients.
	 */
	private void clientConnect(String ip)	{

		//Assign the target IP address
		try	{
			client_ip = InetAddress.getByName(ip);
		} catch (Exception e)	{
			appendToTextArea("Error: Client received invalid IP address.");
		}

		//Initiate sockets to use for audio streaming
		try {
			receive_socket = new DatagramSocket(receive_port);
			send_socket = new DatagramSocket(send_port);
		} catch (Exception e)	{}

		Thread CaptureAudio = new Thread(new CaptureAudio(send_socket, client_ip, receive_port));
		CaptureAudio.start();

		Thread PlayAudio = new Thread(new PlayAudio(receive_socket));
		PlayAudio.start();
	}

	/*
	 * Disconnects the client from the current call.
	 */
	private void clientDisconnect()	{

		try	{
			//stop the threads
			stopCapture = true;
			stopPlay = true;

			//wait for threads to terminate
			try {
				Thread.sleep(200);
			} catch (Exception e) {}

			//close the sockets
			try {
				send_socket.close();
				receive_socket.close();
			} catch (Exception e) {}
		} catch (Exception e)	{}
	}

	/*
	 * Establishes a TCP connection between client and server.
	 */
	private void serverConnect(String ip_string, int port)	{
		try {
			server_ip = InetAddress.getByName(ip_string);
			server_port = port;
			server_socket = new Socket(server_ip, server_port);
			out = server_socket.getOutputStream();
			in =  new BufferedReader(new InputStreamReader(server_socket.getInputStream()));

			appendToTextArea("Connected to server at: " + ip_string);
			connected = true;

			Thread listenLoop = new Thread(new ConnectionListener());
			listenLoop.start();

		} catch (Exception e) {
			appendToTextArea("Error: Connection refused, could not connect to server.");
		}
	}

	private void serverSend(String message)	{
		try	{
			out.write(message.getBytes());
		} catch	(Exception e){			
			appendToTextArea("Error: Connection to server lost.");
		}
	}

	/*
	 * A method for adding text to the text area.
	 * The append method is thread safe.
	 * The setCaretPosition method is not thread safe and
	 * is called on the event-dispatching thread.
	 */
	private void appendToTextArea(String text)	{
		textArea.append(text + "\n");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
		});
	}

	public void actionPerformed(ActionEvent a)	{
		if (a.getSource() == textField)	{
			String text = textField.getText();
			serverSend(text);
			textField.setText("");
		}
	}

	/*
	 * listens to incoming messages from the server
	 */
	private class ConnectionListener implements Runnable {

		public void run() {
			Boolean listen = true;
			try {
				while(listen) {

					String data = in.readLine();

					if (data != null) {
						if (data.substring(0, 4).equals("ca__"))	{
							String ip_string = data.split(" ")[1];
							clientConnect(ip_string);
						} else if (data.substring(0,4).equals("ul__"))	{
							String[] users = data.substring(5).split(" ");
							userList.clearSelection();
							userList.setListData(users);
						} else if (data.substring(0,4).equals("dc__"))	{
							clientDisconnect();
						} else	{
							appendToTextArea(data);
						}
					} else	{
						//when server disconnects, stop listening
						listen = false;
					}
				}
			} catch (Exception e) {
				appendToTextArea("Error: An error occurred with the connection.");
			}
		}
	}

	public static void main(String args[]){
		String server_ip = args[0]; //server ip address
		String server_port = args[1]; //port for tcp connection to server

		VoipClient c = new VoipClient(); 

		c.serverConnect(server_ip, Integer.parseInt(server_port));

		while(connected);
	}
}

