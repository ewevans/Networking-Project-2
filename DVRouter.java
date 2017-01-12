package com.company;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DVRouter {

    public static void main(String[] args) {
        //args are: [0]routerID, [1]portNumber
        //this router's distance vector
        DistanceVector distanceVector = new DistanceVector();

        //distanceVector.AddEntry(args[0], args[0], 0, 0, args[0]);

        //all the neighbor's distance vector
        Map<String, DistanceVector> neighborsDV = new HashMap<>();

        //the direct link cost information for neighboring nodes
        Map<String, String> directLinkCost = new HashMap<String, String>();

        //mapping the neighbor's id to its port number
        Map<String, String> neighborPortNumbers = new HashMap<String, String>();

        //flag set to true when distanceVector has been updated, and should be sent to neighbors
        boolean distanceVectorChanged = false;

        boolean bellmanFord = false;

        //variable to hold the accumulated time the router has paused
        int sleepTotal = 0;

        //variable to hold the number of updates for this router
        int updates = 0;

        //poison reverse, do poison reverse if the flag is true
        boolean poisonReverse = true;

        try {
            //create UDP socket
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[1]));
            System.out.println("Router " + args[0] + " is active. Waiting for adding links...");

            while (true) {
                //reset send to neighbors flag
                distanceVectorChanged = false;

                byte[] receiveData = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);

                //wait until receive message
                socket.receive(receivedPacket);
                receiveData = receivedPacket.getData();
                String receivedString = new String(receiveData, "UTF-8");

                //System.out.println("Received message: " + receivedString);

                //check type of message
                //if direct cost information (first byte 0)
                if (receivedString.charAt(0) == '0') {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    System.out.println("Link updated at " + timestamp);

                    bellmanFord = false;

                    //[0] "0" for flag
                    //[1] char linkedRouter ID
                    String linkedRouterID = receivedString.substring(1, 2);
                    //[2-5] int linkedRouted portNumber
                    String linkedRouterPortNumber = receivedString.substring(2, 6);
                    //[6+] int link cost
                    String linkCost = receivedString.substring(6).trim();


                    //add direct cost info to directLinkCost
                    directLinkCost.put(linkedRouterID, linkCost);

                    //add neighbor to port number
                    neighborPortNumbers.put(linkedRouterID, linkedRouterPortNumber);

                    //if new neighbor, add initial entry to distanceVector
                    distanceVector.UpdateEntry(args[0], linkedRouterID, Integer.parseInt(linkCost), 1, linkedRouterID);

                    //create slot for neighbor in neighborDV
                    neighborsDV.put(linkedRouterID, new DistanceVector());
                    neighborsDV.get(linkedRouterID).UpdateEntry(linkedRouterID,linkedRouterID,0,0,linkedRouterID);

                    //set flag to send neighbors this DV
                    distanceVectorChanged = true;
                }
                else //if distance vector entry (first byte 1)
                {
                    //MESSAGE OUT OF ORDER to have cost at the end
                    bellmanFord = true;

                    //[0] "1" for flag
                    //[1] char linkedRouter ID
                    String EntrySourceID = receivedString.substring(1, 2);
                    //[2] char destination ID
                    String EntryDestinationID = receivedString.substring(2, 3);
                    //[3] number of hops
                    String EntryNumHops = receivedString.substring(3, 4);
                    //[4] next router in route ID
                    String EntryNextRouterID = receivedString.substring(4, 5);
                    //[5+]  distance of route
                    String EntryDistance = receivedString.substring(5).trim();

                    //pause sender link cost * 10 ms
                    int sleepTime = 10 * Integer.parseInt(directLinkCost.get(EntrySourceID));
                    Thread.sleep(sleepTime);
                    sleepTotal = sleepTotal + sleepTime;

                    //System.out.println("Pausing for: " + sleepTime + " ms with a total of " + sleepTotal + " ms paused.");

                    //update neighbors DV
                    DistanceVector neighbor = neighborsDV.get(EntrySourceID);
                    //if (neighbor != null)
                        neighbor.UpdateEntry(EntrySourceID, EntryDestinationID, Integer.parseInt(EntryDistance),
                                Integer.parseInt(EntryNumHops), EntryNextRouterID);


                    //check if learn about new distant router
                    if (distanceVector.EntryExists(EntryDestinationID) == false && !args[0].equals(EntryDestinationID)){

                    distanceVector.UpdateEntry(args[0], EntryDestinationID,
                            9999,
                            0, EntrySourceID);
                    }

                }

                //bellman ford

                if (bellmanFord)
                {
                    //save distance vector for test after to see if changed to send out
                    DistanceVector oldDV = new DistanceVector();
                    for(int i = 0; i < distanceVector.DVList.size(); i++)
                    {
                        oldDV.UpdateEntry(distanceVector.DVList.get(i).sourceID,
                                distanceVector.DVList.get(i).destinationID,
                                distanceVector.DVList.get(i).distance,
                                distanceVector.DVList.get(i).numHops,
                                distanceVector.DVList.get(i).nextRouterID);
                    }

                    //loop through each entry in the distanceVector
                    for (DistanceVector.DVEntry entry : distanceVector.DVList) {

                        //skip the entry that contain route to itself
                        if (entry.sourceID.equals(entry.destinationID))
                            continue;

                        entry.distance = 99999;

                        //loop through all the neighbor distance vectors
                        for (Map.Entry<String, DistanceVector> neighborDV : neighborsDV.entrySet())
                        {
                            //if poison reverse has been flagged, check if skipping neighbor
                            if (poisonReverse)
                            {
                                //if the neighbor in question has a next hop back through this router, continue
                                String poisonEntry = neighborDV.getValue().GetDVNextHop(entry.destinationID);

                                if (poisonEntry != null && poisonEntry.equals(args[0]))
                                {
                                    continue;
                                }
                            }

                            DistanceVector.DVEntry revNeighborEntry = neighborDV.getValue().GetDVEntry(entry.destinationID);

                            //if the neighbor has a DV entry for the current entry's destination
                            if (revNeighborEntry != null)
                            {
                                //System.out.println("neighbor " + neighborDV.getValue().GetDVEntry(entry.destinationID).sourceID + " to " + neighborDV.getValue().GetDVEntry(entry.destinationID).destinationID
                                 //       + " with distance of " + neighborDV.getValue().GetDVEntry(entry.destinationID).distance + " + " + Integer.parseInt(directLinkCost.get(neighborDV.getKey())) + " versus entry distance of " + entry.distance);

                                // if cost of direct link + neighbor route < current DV entry distance
                                int directLink = Integer.parseInt(directLinkCost.get(neighborDV.getKey()));
                                int neighborDistance = revNeighborEntry.distance;

                                if (directLink + neighborDistance < entry.distance)
                                {
                                    entry.distance = directLink + neighborDistance;
                                    entry.numHops = revNeighborEntry.numHops + 1;

                                    if (entry.numHops > 9)
                                        entry.numHops = 9;

                                    entry.nextRouterID = neighborDV.getKey();
                                }
                            }
                        }

                        //if the distance vector has changed, flag to send messages to neighbors
                        if (!oldDV.equals(distanceVector))
                        {
                            distanceVectorChanged = true;
                        }
                    }
                }

                //send distanceVector to neighbors
                if ( distanceVectorChanged)
                {
                    updates++;

                    distanceVector.PrintDV(updates);
                    //System.out.println("Sent messages.");
                    //get localhost address
                    InetAddress localHost = InetAddress.getLocalHost();

                    //method of DistanceVector packs distance vector into a list of ready-to-send DVEntry byte arrays
                    List<byte[]> packedDVEntries = distanceVector.PackDV();

                    //for each neighbor, send each DV entry
                    for (String portNumber : neighborPortNumbers.values()) {
                        for (byte[] sendData : packedDVEntries) {
                            //System.out.println("SENT MESSAGE to " + portNumber);

                            DatagramPacket packet = new DatagramPacket(sendData, sendData.length,
                                    localHost, Integer.parseInt(portNumber));
                            socket.send(packet);
                            //System.out.println("Sent message to ");
                        }
                    }
                }

            }//end infinite loop

        } //end try
        catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }

    }
}