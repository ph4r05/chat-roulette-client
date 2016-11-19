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
public class Message {
    private JSONObject body;

    public Message() {
    }

    public Message(JSONObject body) {
        this.body = body;
    }

    public void setBody(JSONObject body) {
        this.body = body;
    }

    public JSONObject getBody() {
        return body;
    }
    
}
