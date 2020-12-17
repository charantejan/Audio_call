package com.example.audio_call;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;

import static androidx.core.content.ContextCompat.getSystemService;
import static java.nio.ByteOrder.BIG_ENDIAN;

public class AudioCall {
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private InetAddress address;// Address to call
    private int port = 50000; // Port the packets are addressed to
    private boolean mic = false; // Enable mic?
    private boolean speakers = false; // Enable speakers?
    private boolean UDP;
//    private DatagramSocket socket = null;
    private Socket TCPsocket = null;
    private OutputStream out1 = null;
    private BufferedOutputStream buff;
    private DataOutputStream dataOutputStreamInstance;
    private InputStream in1 = null;
    private BufferedInputStream buff_in;
    private DataInputStream dataInputStreamInstance;
    public boolean getendcall = true; //If the call is ended and should return to previous screen
    private Context mContext;

    private packetizer packet;

    public AudioCall(final int port, boolean UDP, Context mContext) {
        try {
            this.address = InetAddress.getByName("192.168.1.4"); //Server address here
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.mContext = mContext;
        this.port = port;
        this.UDP = UDP;
        packet = new packetizer(0);
        if(!UDP) {
            //Using a new thread because network activities cannot happen on the main thread in android
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d("socket123", "socket");
                        TCPsocket = new Socket(address, port);
                        Log.d("socket123", String.valueOf(port));
                        //Setup all the buffers
                        out1 = TCPsocket.getOutputStream();
                        in1 = TCPsocket.getInputStream();
                        buff = new BufferedOutputStream(out1); //out1 is the socket's outputStream
                        dataOutputStreamInstance = new DataOutputStream(buff);
                        buff_in = new BufferedInputStream(in1);
                        dataInputStreamInstance = new DataInputStream(buff_in);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            });
            thread.start();
            try {
                thread.join();
                Log.d("check123", "here123");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setPackType() { //Can use this function to change the network connection type in between the call based on some metric. Did not check.
        UDP = !(UDP);
        if (UDP){
            try {
                TCPsocket = new Socket(address, port);
                out1 = TCPsocket.getOutputStream();
                in1 = TCPsocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            buff = new BufferedOutputStream(out1); //out1 is the socket's outputStream
            dataOutputStreamInstance = new DataOutputStream (buff);
            buff_in = new BufferedInputStream(in1);
            dataInputStreamInstance = new DataInputStream(buff_in);
            startMicTCP();
            startSpeakersTCP();
        }
        else{
            startMic();
            startSpeakersTCP();
        }
    }

    public void startCall() {

        startMic();
        startSpeakers();
    }

    public void endCall() {

        muteMic();
        muteSpeakers();
    }

    public void muteMic() {

        mic = false;
    }

    public void unmuteMic(){
        mic = true;
        if(UDP){
            startMic();
        }
        else {
            startMicTCP();
        }
    }

    public void muteSpeakers() {

        speakers = false;
    }

    public void startMic() {
        // Creates the thread for capturing and transmitting audio
        mic = true;
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Create an instance of the AudioRecord class
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                } catch (SocketException e) {
                    e.printStackTrace();
                    return;
                }
                //Record the audio
                AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];
                // Create a socket and start recording
                audioRecorder.startRecording();
                while(mic && UDP) {
                    // Capture audio from the mic and transmit it
                    bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);  //also should add the headers required
                    //The following code is to add the length in 4 bytes to the packet. Required in TCP connection if you use recv function in multiplex.py(server side).
//                    byte[] len = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(bytes_read).array();
//                    byte[] toSend = new byte[4+bytes_read];
//                    System.arraycopy(len, 0, toSend, 0, 4);
//                    System.arraycopy(buf, 0, toSend, 0, bytes_read);
                    Log.d("port123", "Before packetize");
                    byte[] pac = packet.packetize(buf, 1, 0);
                    Log.d("port123", "after packetize");
                    DatagramPacket packet = new DatagramPacket(pac, bytes_read, address, port);
                    Log.d("sent", String.valueOf(bytes_read));
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    bytes_sent += bytes_read;
                }
                // Stop recording and release resources
                audioRecorder.stop();
                audioRecorder.release();
                byte[] emptybuf = new byte[BUF_SIZE];
                DatagramPacket packet = new DatagramPacket(emptybuf, 0, address, port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.disconnect();
                socket.close();
                return;
            }
        });
        thread.start();
    }

    public void startMicTCP() {
        mic = true;
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Create an instance of the AudioRecord class


                AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];
                Log.d("bytes_read", String.valueOf(bytes_read));
                audioRecorder.startRecording();
                while (mic && !UDP){
                    bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);  //also should add the headers required for our case
                    Log.d("bytes_read", String.valueOf(bytes_read));
                    //The following code is to add the length in 4 bytes to the packet. Required in TCP connection if you use recv function in multiplex.py(server side).
//                    byte[] len = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(bytes_read).array();
//                    byte[] toSend = new byte[4+bytes_read];
//                    System.arraycopy(len, 0, toSend, 0, 4);
//                    System.arraycopy(buf, 0, toSend, 4, bytes_read);
                    try {
                        dataOutputStreamInstance.write(buf);
                        dataOutputStreamInstance.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    bytes_sent += bytes_read;
                }

                // Stop recording and release resources
                audioRecorder.stop();
                audioRecorder.release();
                try {
                    buff.close();
                    dataOutputStreamInstance.close();
                    out1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        });
        thread.start();
    }

    public void startSpeakers() {
        // Creates the thread for receiving and playing back audio
        if(!speakers) {

            speakers = true;
            Thread receiveThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    // Create an instance of AudioTrack, used for playing back audio
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                    track.play();
                    try {
                        // Define a socket to receive the audio
                        DatagramSocket socket = new DatagramSocket(5000);
                        byte[] buf = new byte[BUF_SIZE];
                        while(speakers && UDP) {
                            // Play back the audio received from packets
                            DatagramPacket packet1 = new DatagramPacket(buf, BUF_SIZE);
                            socket.receive(packet1);
                            Log.d("sent", "recv");
                            Object[] dataPack = packet.depacketize(packet1.getData());
                            if((int) dataPack[0] == 1){
                                Log.d("check", "de-packet");
                                byte[] audio = (byte[]) dataPack[1];
                                track.write(audio, 0, audio.length);
                                byte[] ack = (byte[]) dataPack[2];
                                DatagramPacket packet_ack = new DatagramPacket(ack, ack.length, address, port);
                                try {
                                    socket.send(packet_ack);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                        // Stop playing back and release resources
                        socket.disconnect();
                        socket.close();
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    }
                    catch(SocketException e) {
                        speakers = false;
                    }
                    catch(IOException e) {
                        speakers = false;
                    }
                }
            });
            receiveThread.start();
        }
    }

    public void startSpeakersTCP() {
        // Creates the thread for receiving and playing back audio
        if(!speakers) {

            speakers = true;
            Thread receiveThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // Create an instance of AudioTrack, used for playing back audio
                    AudioManager m_amAudioManager;
                    m_amAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    m_amAudioManager.setMode(AudioManager.MODE_IN_CALL);
                    m_amAudioManager.setSpeakerphoneOn(false);
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                    track.play();
                    try {
                        // Define a socket to receive the audio
                        byte[] buf = new byte[BUF_SIZE];
                        while(speakers && !UDP) {
                            // Play back the audio received from packets
                            int rec;
                            rec = dataInputStreamInstance.read(buf);
                            Log.d("rec123", String.valueOf(rec));
                            if (rec > 0)
                                track.write(buf, 4, rec);
                        }
                        // Stop playing back and release resources
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    }
                    catch(SocketException e) {
                        speakers = false;
                    }
                    catch(IOException e) {
                        speakers = false;
                    }
                }
            });
            receiveThread.start();
        }
    }

}
