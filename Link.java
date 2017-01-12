package com.company;


//Link.java

//This program is used to tell DVRouters a direct cost link


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Link {

    public static void main(String[] args) {
	//args are: [0]router1Id, [1]router1PortNumber, [2]router2Id, [3]router2PortNumber, [4]cost
        try {

            //make UDP socket to send through
            DatagramSocket socket = new DatagramSocket(7111);


            // create message to send to router1
            //only sending "0" for direct cost flag, other router ID and port, and cost
            byte[] router1Message = new byte[1024];
            String router1String = "0" + args[2] + args[3] + args[4];
            router1Message = router1String.getBytes();
            DatagramPacket router1Packet = new DatagramPacket(router1Message, router1Message.length,
                    InetAddress.getLocalHost(), Integer.parseInt(args[1]));

            // create message to send to router2
            //only sending "0" for direct cost flag, other router ID and port, and cost
            byte[] router2Message = new byte[1024];
            String router2String = "0" + args[0] + args[1] + args[4];
            router2Message = router2String.getBytes();
            DatagramPacket router2Packet = new DatagramPacket(router2Message, router2Message.length,
                    InetAddress.getLocalHost(), Integer.parseInt(args[3]));

            //send packets
            socket.send(router1Packet);
            socket.send(router2Packet);

            //close socket
            socket.close();
        }
        catch (Exception ex)
        {
            System.out.print(ex);
        }


    }
}
