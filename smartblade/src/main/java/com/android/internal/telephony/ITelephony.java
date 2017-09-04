package com.android.internal.telephony;

/**
 * Created by ajrunhaar on 5/26/2017.
 */

public interface ITelephony {

    boolean endCall();

    void answerRingingCall();

    void silenceRinger();
}
