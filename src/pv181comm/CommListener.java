/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pv181comm;

import org.json.JSONObject;

/**
 *
 * @author dusanklinec
 */
public interface CommListener {
    void onCommand(String cmd, JSONObject msg);
    void onError(String error, JSONObject msg);
    void onAck(JSONObject msg);
    void onExit(JSONObject msg);
    
}
