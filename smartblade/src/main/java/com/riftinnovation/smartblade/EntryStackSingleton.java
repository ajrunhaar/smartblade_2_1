package com.riftinnovation.smartblade;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ajrunhaar on 7/27/2017.
 */

public class EntryStackSingleton {
    private static EntryStackSingleton instance = null;
    private List<Entry> EntryStack= Collections.synchronizedList(new ArrayList<Entry>());

    private EntryStackSingleton(){

    }

    public static EntryStackSingleton getInstance(){
        if(instance == null){
            synchronized (EntryStackSingleton.class) {
                instance = new EntryStackSingleton();
            }
        }
        return instance;
    }

    public List<Entry> getEntryStack(){
        return EntryStack;
    }


}
