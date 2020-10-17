package com.example.audio_call;

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
import java.util.concurrent.locks.Lock;

public class AudioCall {
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private InetAddress address; // Address to call
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
    public boolean getendcall = true;
    public AudioCall(InetAddress address, boolean UDP) {

        this.address = address;
        this.UDP = UDP;
    }

    public void setPackType() {

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
                AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);
                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];
                // Create a socket and start recording
                audioRecorder.startRecording();
                while(mic && UDP) {
                    // Capture audio from the mic and transmit it
                    bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);  //also should add the headers required for our case
                    DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
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
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);
                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];

                while (mic && !UDP){
                    bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);  //also should add the headers required for our case
                    try {
                        dataOutputStreamInstance.write(buf);
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
                        DatagramSocket socket = new DatagramSocket(port);
                        byte[] buf = new byte[BUF_SIZE];
                        while(speakers && UDP) {
                            // Play back the audio received from packets
                            DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                            socket.receive(packet);
                            track.write(packet.getData(), 0, BUF_SIZE);
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
                            if (rec > 0)
                                track.write(buf, 0, rec);
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
