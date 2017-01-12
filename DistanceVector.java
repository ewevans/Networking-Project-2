package com.company;

import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Created by Ethan on 11/17/2016.
 */
public class DistanceVector {

    class DVEntry
    {
        String sourceID;
        String destinationID;
        int distance;
        int numHops;
        String nextRouterID;
    }


    ArrayList<DVEntry> DVList = new ArrayList<>();

    //creates an entry with the given params
    void AddEntry(String sID, String dest, int dist, int hops, String next)
    {
        DVEntry entry = new DVEntry();
        entry.sourceID = sID;
        entry.destinationID = dest;
        entry.distance = dist;
        entry.numHops = hops;
        entry.nextRouterID = next;
        DVList.add(entry);
    }

    //returns true if an existing entry was updated
    //returns false if it added a new entry
    boolean UpdateEntry(String sID, String dest, int dist, int hops, String next)
    {
        if (EntryExists(dest) == false)
        {
            AddEntry(sID, dest, dist, hops, next);
            return false;
        }
        else
        {
            for (DVEntry entry : DVList)
            {
                if (entry.destinationID.equals(dest))
                {
                    entry.distance = dist;
                    entry.numHops = hops;
                    entry.nextRouterID = next;
                }
            }
            return true;
        }
    }

    //returns true if the distance vector has an entry to the destination
    boolean EntryExists(String dest)
    {
        for(DVEntry entry : DVList)
        {
            if (entry.destinationID.equals(dest))
            {
                return true;
            }
        }
        return false;
    }

    void PrintDV(int updates)
    {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println("- - - - - time of update: " + timestamp);
        for (DVEntry entry : DVList)
        {
            System.out.println(entry.sourceID + " " + entry.destinationID + " "
                    + entry.distance + " " + entry.numHops + " " + entry.nextRouterID);
        }
        System.out.println("- - - - - # of updates: " + updates);
    }

    ArrayList<byte[]> PackDV()
    {
        ArrayList<byte[]> listDVEntries = new ArrayList<byte[]>();
        for(DVEntry entry : DVList)
        {
            if (!entry.sourceID.equals(entry.destinationID)) {


                byte[] sendData = new byte[1024];

                //[0] "1" for flag
                //[1] char linkedRouter ID
                //[2] char destination ID
                //[3] number of hops
                //[4] next router in route ID
                //[5+]  distance of route

                String sendString = "1" + entry.sourceID + entry.destinationID
                        + entry.numHops + entry.nextRouterID + entry.distance;

                sendData = sendString.getBytes();
                listDVEntries.add(sendData);
           }
        }

        return listDVEntries;
    }

    DVEntry GetDVEntry(String dest)
    {
        for (DVEntry entry : DVList)
        {
            if (entry.destinationID.equals(dest))
            {
                return entry;
            }
        }
        return null;
    }

    String GetDVNextHop(String dest)
    {
        for (DVEntry entry : DVList)
        {
            if (entry.destinationID.equals(dest))
            {
                return entry.nextRouterID;
            }
        }
        return null;
    }


    boolean equals(DistanceVector otherDV)
    {
        //if same number of entries
        if (otherDV.DVList.size() == this.DVList.size())
        {
            //if each of those entries are the same
            for (int i = 0; i < this.DVList.size(); i++)
            {
                if (!this.DVList.get(i).sourceID.equals(otherDV.DVList.get(i).sourceID)
                        || !this.DVList.get(i).destinationID.equals(otherDV.DVList.get(i).destinationID)
                        || this.DVList.get(i).distance != otherDV.DVList.get(i).distance
                        || this.DVList.get(i).numHops != otherDV.DVList.get(i).numHops
                        || !this.DVList.get(i).nextRouterID.equals(otherDV.DVList.get(i).nextRouterID))
                {
                    return false;
                }
            }
            return true;
        }
        return false;

    }
}
