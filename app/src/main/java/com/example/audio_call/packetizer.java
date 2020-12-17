package com.example.audio_call;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.nio.ByteOrder.BIG_ENDIAN;
// The following code can be used to packetize the data as required.
public class packetizer {
    private int packet_num_sent;
    private int last_packet_received;
    private int packets_lost; //Should be an array or map, for a group call, should be UPDATED
    private int id;
    private int journey_time;
    private int packet_ack;//Should be an array or map for a group call, should be UPDATED
    private int our_packs_lost; //Should also be an array or map in a group call, should be UPDATED
    public packetizer(int id){
        packet_num_sent = 0;
        last_packet_received = 0;
        packets_lost = 0;
        this.id = id;
        journey_time =0;
        packet_ack = 0;
        our_packs_lost = 0;
    }

    public byte[] packetize( byte[] data, int type, int pack_num){

        byte[] psent = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(packet_num_sent).array();
        byte[] pack = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(pack_num).array();
        byte[] id = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(this.id).array();
        byte[] num_packets_lost = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(packets_lost).array();
        byte[] pack_type = ByteBuffer.allocate(2).order(BIG_ENDIAN).putShort((short) type).array();
        int dateInSec = (int) (System.currentTimeMillis() / 1000);
        byte[] time = ByteBuffer.allocate(4).putInt(dateInSec).array();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        if(type != 0){
            try {
                outputStream.write(pack_type); //Packet type
                outputStream.write(id);        //ID, is not implemented yet
                outputStream.write(psent);     //Packet number
                outputStream.write(time);      //Time spent
                outputStream.write(data);      //Data(Audio)
                byte c[] = outputStream.toByteArray( );
                packet_num_sent+=1;
                return c;
            } catch (IOException e) {
                e.printStackTrace();
                byte c[] = new byte[0];
                return c;
            }
        }
        else{
            try {
                outputStream.write(pack_type); //Packet type
                outputStream.write(id);        //ID, is not implemented yet
                outputStream.write(pack);      //The packet number for which this ack is addressed to
                outputStream.write(num_packets_lost); //No.of packets lost from that specific client
                outputStream.write(time);      //Timestamp
                byte c[] = outputStream.toByteArray( );
                return c;
            } catch (IOException e) {
                e.printStackTrace();
                byte c[] = new byte[0];
                return c;
            }

        }
    }

    public Object[] depacketize(byte[] packet){
        byte[] packType = Arrays.copyOfRange(packet, 0, 2);
        int pack_type = ByteBuffer.wrap(packType).getShort();
        byte[] senderId = Arrays.copyOfRange(packet, 2, 6);
        int sender_id = ByteBuffer.wrap(senderId).getInt();
        if(pack_type!=0){ //If packet type is data, remove all the required data and send data + Ack packet to be sent.
            byte[] packetNumRec = Arrays.copyOfRange(packet, 6, 10);
            int packet_rec = ByteBuffer.wrap(packetNumRec).getInt();
            byte[] TimeStamp = Arrays.copyOfRange(packet, 10, 14);
            int timeTaken = ByteBuffer.wrap(TimeStamp).getInt();
            int dateInSec = (int) (System.currentTimeMillis() / 1000);
            journey_time = dateInSec - timeTaken;
            packets_lost += (packet_rec - last_packet_received - 1);
            last_packet_received = packet_rec;
            byte[] data = Arrays.copyOfRange(packet, 14, packet.length); //Should be ID, should be UPDATED.
            byte[] new_packet = packetize(data, 0, packet_rec);
            return new Object[]{pack_type, data, new_packet};
        }
        else{ //If the packet received is ACK, get the data and update the stats.
            //ID should also be checked and updated accordingly.
            byte[] packetNumRec = Arrays.copyOfRange(packet, 6, 10);
            int packet_rec = ByteBuffer.wrap(packetNumRec).getInt();
            packet_ack = packet_rec;
            byte[] pacsLost = Arrays.copyOfRange(packet, 10, 14);
            int packetsLost = ByteBuffer.wrap(pacsLost).getInt();
            our_packs_lost = packetsLost;
            return new Object[]{pack_type};
        }
    }

    public int get_journeyTime(){
        return this.journey_time;
    } //For avg trip time, should take in ID as an argument, should be UPDATED.

    public int our_packetsLost(){ //For a group call, should take an input ID, should be UPDATED
        return this.our_packs_lost;
    } //For number of packets lost from our side to the other clients

    public int AckReceivedFor(){//For a group call, should take an input ID, should be UPDATED
        return this.packet_ack;
    }//The last packet for which an ack is received
}
