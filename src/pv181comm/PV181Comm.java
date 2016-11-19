package pv181comm;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONObject;

/**
 * Diffie-Hellman chat roulette.
 * 
 * @author dusanklinec
 */
public class PV181Comm implements CommListener {
    
    public static final String FIELD_CMD = "cmd";
    public static final String FIELD_SUB = "sub";
    
    public static final String FIELD_COMM = "comm";
    public static final String FIELD_PAIR = "pair";
    public static final String FIELD_UCO = "uco";
    
    public static final String SUB_DH1 = "dh1";
    public static final String SUB_DH2 = "dh2";
    public static final String SUB_DH_CONF = "dhc";
    
    public static final String FIELD_DHPUB = "dhpub";
    public static final String FIELD_DHVERIF = "dhverif";
    
    // DH state
    public final int DHSTATE_INIT = 0;
    public final int DHSTATE_DH1_SENT = 1;
    public final int DHSTATE_DH1_RECEIVED = 2;
    public final int DHSTATE_DH2_SENT = 3;
    public final int DHSTATE_DH2_RECEIVED = 4;
    public final int DHSTATE_DONE = 5;
    
    private Comm comm;
    private String peerUco = null;
    private int dhState = DHSTATE_INIT;
    
    private KeyPair kp;
    private PublicKey remotePublic;
    private byte[] sharedKey;
    private boolean dhc_sent = false;
    
    /**
     * Main.
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        PV181Comm main = new PV181Comm();
        main.work();
    }
    
    /**
     * Main worker method.
     * 
     * @throws IOException
     * @throws InterruptedException 
     */
    public void work() throws IOException, InterruptedException{
        comm = new Comm("94.177.231.131", 44333);
        comm.setUco("your_uco");
        comm.setSession("alpha");
        comm.setHandler(this);
        comm.connect();
        
        while(true){
            Thread.sleep(200);
            // 
        }
    }
    
    /**
     * Returns true if my UCO is smaller than peer has. Thus I am the DH initiator.
     * @return 
     */
    public boolean amIInitiator(){
        return comm.getUco().compareTo(peerUco) < 0;
    }
    
    /**
     * Unique string for this connection with given peer
     * @return 
     */
    public String getConnectionId(){
        if (amIInitiator()){
            return comm.getUco() + "|" + peerUco;
        } else {
            return peerUco + "|" + comm.getUco();
        }
    }
    
    /**
     * Resets internal DH state.
     * Called after new pairing is established.
     */
    public void resetState(){
        kp=null;
        remotePublic=null;
        dhState = DHSTATE_INIT;
    }
    
    /**
     * Prepares sub message to send to the peer.
     * @param sub
     * @return 
     */
    public JSONObject preparePeerMsg(String sub){
        JSONObject msg = comm.getSkeleton();
        msg.put(FIELD_CMD, FIELD_COMM);
        if (sub != null){
            msg.put(FIELD_SUB, sub);
        }
        
        return msg;
    }
    
    public PublicKey parseDHPublicKeyBase64(String dhPubBase64) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] dhPubByte = DatatypeConverter.parseBase64Binary(dhPubBase64);

        KeyFactory kf = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509ks = new X509EncodedKeySpec(dhPubByte);
        PublicKey pubKeyA = kf.generatePublic(x509ks);
        return pubKeyA;
    }
    
    public String serializeDHPublicKey(PublicKey pubKey){
        return DatatypeConverter.printBase64Binary(pubKey.getEncoded());
    }
    
    /**
     * Handler called by the listening thread on a new message.
     * @param cmd
     * @param msg 
     */
    @Override
    public void onCommand(String cmd, JSONObject msg) {
        // New pairing?
        if (FIELD_PAIR.equals(cmd)){
            // New pairing -> reset internal state
            peerUco = msg.getString(FIELD_UCO);
            resetState();
            
            System.out.println(String.format(
                    "New pairing with %s, connection ID: %s, I am initiator: %s", 
                    peerUco, getConnectionId(), amIInitiator()));
            
            // TODO: start new DH protocol here, 
            // goo.gl/Lus40Y
            if (amIInitiator()){
                try {
                    // Send DH part 1 g^x
                    // Prepare the message
                    KeyPairGenerator kpGen = KeyPairGenerator.getInstance("DH");
                    kp = kpGen.generateKeyPair();
                    PublicKey pubKey = kp.getPublic();
                    String pubKeyBase64 = serializeDHPublicKey(pubKey);
                    
                    System.out.println(String.format("Generated PubKey %s data: %s ",
                            pubKey.getFormat(), pubKeyBase64));
                    
                    JSONObject dh1 = preparePeerMsg(SUB_DH1);
                    dh1.put(FIELD_DHPUB, pubKeyBase64);
                    comm.enqueue(dh1);
                    dhState = DHSTATE_DH1_SENT;
                    
                    // Wait for DH2 message.
                    // ...
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(PV181Comm.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            } else {
                // Wait for DH1 message, then respond with DH2 message.
                
            }
            
        // Message from the peer?    
        } else if (FIELD_COMM.equals(cmd)){ 
            if (msg.has(FIELD_SUB)){
                final String sub = msg.getString(FIELD_SUB);
                System.out.println(String.format("Sub: %s peer %s msg %s", sub, peerUco, msg));
                
                // DH1 message? do DH2 message...
                if (SUB_DH1.equals(sub)){
                    try {
                        dhState = DHSTATE_DH1_RECEIVED;
                        
                        // Get Dh pubkey from the received message
                        String dhPubBase64 = msg.getString(FIELD_DHVERIF);
                        byte[] dhPubByte = DatatypeConverter.parseBase64Binary(dhPubBase64);
                        
                        // Use keyfactory to transform byte encoded public key
                        // to the usable object.
                        KeyFactory kf = KeyFactory.getInstance("DH");
                        X509EncodedKeySpec x509ks = new X509EncodedKeySpec(dhPubByte);
                        PublicKey pubKeyA = kf.generatePublic(x509ks);
                        
                        // Generate my key-pair, take public key, convert it to 
                        // bytes, then to base64 string.
                        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("DH");
                        kp = kpGen.generateKeyPair();
                        PublicKey pubKey = kp.getPublic();
                        String pubKeyBase64 = DatatypeConverter.printBase64Binary(pubKey.getEncoded());
                        
                        // TODO: FIX THIS:
                        JSONObject dh2 = preparePeerMsg(SUB_DH2);
                        PublicKey dhPk = null; // fill this in
                        dh2.put(FIELD_DHPUB, DatatypeConverter.printBase64Binary(dhPk.getEncoded()));
                        comm.enqueue(dh2);
                        dhState = DHSTATE_DH2_SENT;
                        
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(PV181Comm.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvalidKeySpecException ex) {
                        Logger.getLogger(PV181Comm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                } else if (SUB_DH2.equals(sub)){
                    dhState = DHSTATE_DH2_RECEIVED;
                    // TODO: finish DH.
                    // TODO: compute confirmation = HMAC(input=getConnectionId(), key=DH)
                    
                } else if (SUB_DH_CONF.equals(sub)){
                    // TODO: check the DH confirmation.
                }
                
            } else {
                System.out.println(String.format("New generic message from peer %s, %s", peerUco, msg));
            }
        
        } else {
            // Unknown message
            System.out.println(String.format("cmd: %s, msg: %s", cmd, msg));
        }
    }

    @Override
    public void onError(String error, JSONObject msg) {
        System.out.println(String.format("error: %s, msg: %s", error, msg));
    }

    @Override
    public void onAck(JSONObject msg) {
        System.out.println(String.format("Ack: %s", msg));
    }

    @Override
    public void onExit(JSONObject msg) {
        System.out.println(String.format("Exit msg: %s", msg));
    }   
}
