/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pv181comm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 *
 * @author dusanklinec
 */
public class Comm {
    private String ip;
    private int port;
    
    private String uco;
    private String session;
    
    private Socket clientSocket;
    private BufferedWriter writer;
    private BufferedReader reader;
    
    private Thread listener;
    private Thread sender;
    private CommListener handler;
    private SecureRandom rand;
    
    private volatile boolean running = true;
    private final ConcurrentLinkedQueue<Message> toSend = new ConcurrentLinkedQueue<>();
    
    public Comm(){
        init("localhost", 44333);
    }
    
    public Comm(String ip, int port){
        init(ip, port);
    }
    
    public final void init(String ip, int port){
        this.ip = ip;
        this.port = port;
    }
    
    public void connect() throws IOException{
        rand = new SecureRandom();
        clientSocket = new Socket(ip, port);
        clientSocket.setSoTimeout(500);
        clientSocket.setReuseAddress(true);
        
        writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        
        listener = new Thread(new Runnable() {
            @Override
            public void run() {
                listenerMain();
            }
        }, "listener");
        listener.start();
        
        sender = new Thread(new Runnable() {
            @Override
            public void run() {
                senderMain();
            }
        }, "sender");
        sender.start();
        
        // Send registration packet
        JSONObject js = getSkeleton();
        js.put("cmd", "connect");
        enqueue(js);
    }
    
    public void enqueue(Message msg){
        toSend.add(msg);
    }
    
    public void enqueue(JSONObject msg){
        enqueue(new Message(msg));
    }
    
    public JSONObject getSkeleton(){
        JSONObject resp = new JSONObject();
        resp.put("uco", this.uco);
        resp.put("session", this.session);
        resp.put("nonce", rand.nextInt());
        return resp;
    }
    
    protected void listenerMain(){
        while(this.running){
            try {
                String line = reader.readLine();
                JSONObject js = new JSONObject(line);
                
                if (js.has("cmd")){
                    final String cmd = js.getString("cmd");
                    
                    // Handle ping right here, protocol important...
                    if ("ping".equals(cmd)){
                        JSONObject resp = getSkeleton();
                        resp.put("cmd", "pong");
                        enqueue(resp);
                        continue;
                    }
                    
                    handler.onCommand(cmd, js);
                    
                } else if (js.has("ack")){
                    handler.onAck(js.getJSONObject("msg"));
                } else if (js.has("error")){
                    handler.onError(js.getString("error"), js);
                } else if (js.has("exit")){
                    handler.onExit(js);
                }
                
            } catch(Exception e){
                
            }
        }
    }
    
    protected void senderMain(){
        while(this.running){
            try{
                Message msg = this.toSend.poll();
                if (msg == null){
                    Thread.sleep(20);
                }
                
                JSONObject js = msg.getBody();
                writer.write(js.toString());
                writer.newLine();
                writer.flush();

            } catch(Exception e){
                
            }
        }
    }
    
    public void disconnect() {
        try {
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Comm.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.running = false;
        }
    }

    public void setUco(String uco) {
        this.uco = uco;
    }

    public void setSession(String session) {
        this.session = session;
    }
    
    public String getSession() {
        return session;
    }

    public void setHandler(CommListener handler) {
        this.handler = handler;
    }

    public String getUco() {
        return uco;
    }
    
    
    
    
}
