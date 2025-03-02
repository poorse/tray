package qz.communication;

import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.DeviceUtilities;
import qz.utils.NetworkUtilities;
import qz.ws.SocketConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class SocketIO implements DeviceListener {

    private static final Logger log = LogManager.getLogger(SocketIO.class);

    private String host;
    private int port;
    private Charset encoding;

    private Socket socket;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;

    private SocketConnection websocket;

    public SocketIO(String host, int port, Charset encoding, SocketConnection websocket) {
        this.host = host;
        this.port = port;
        this.encoding = encoding;
        this.websocket = websocket;
    }

    public boolean open() throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(NetworkUtilities.SOCKET_TIMEOUT);
        dataOut = new DataOutputStream(socket.getOutputStream());
        dataIn = new DataInputStream(socket.getInputStream());

        return socket.isConnected();
    }

    public boolean isOpen() {
        return socket.isConnected();
    }

    public void sendData(JSONObject params) throws JSONException, IOException {
        log.debug("Sending data over [{}:{}]", host, port);
        dataOut.write(DeviceUtilities.getDataBytes(params, encoding));
        dataOut.flush();
    }

    public String processSocketResponse() throws IOException {
        byte[] response = new byte[1024];
        ArrayList<Byte> fullResponse = new ArrayList<>();
        do {
            int size = dataIn.read(response);
            for(int i = 0; i < size; i++) {
                fullResponse.add(response[i]);
            }
        }
        while(dataIn.available() > 0);
        if(fullResponse.size() > 0) {
            return new String(ArrayUtils.toPrimitive(fullResponse.toArray(new Byte[0])), encoding);
        }
        return null;
    }

    @Override
    public void close() {
        // Remove orphaned reference
        websocket.removeNetworkSocket(String.format("%s:%s", host, port));

        try {
            dataOut.close();
        } catch(IOException e) {
            log.warn("Could not close socket output stream", e);
        }
        try {
            socket.close();
        } catch(IOException e) {
            log.warn("Could not close socket", e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

}
