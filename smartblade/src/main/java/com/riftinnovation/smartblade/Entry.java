package com.riftinnovation.smartblade;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by ajrunhaar on 7/27/2017.
 */

public class Entry {
    private String UserID;
    private String Code;
    private JSONObject Entry;
    private Date TimeStamp;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");

    public Entry(String UserID, JSONObject Entry){
        this.UserID = UserID;
        this.Entry = Entry;
        this.TimeStamp = Calendar.getInstance().getTime();
    }

    public String getUserID() {
        return UserID;
    }


    public JSONObject getEntry(){
        return Entry;
    }
    public String getTimeStamp() {


        return dateFormat.format(this.TimeStamp);

    }
    public String toString(){
        return UserID + " | " + Code + " | " + Entry.toString() + " | " + getTimeStamp();
    }
}
