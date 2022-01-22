import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientConnection extends Thread {
    private Socket socket;

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    private int clientId;
    private String clientName;
    private Server server;

    public ClientConnection(int clientId, Socket socket, Server server) {
        this.clientId = clientId;
        this.socket = socket;
        this.server = server;
    }


    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //BufferReader读取一行
            String line;
            while ((line = br.readLine()) != null) {
                //用户还没有上线，读取的一行是用户信息
                if (isNotOnlineYet()) {
                    clientName = line;
                    server.registerClient(this);
                } else {
                    //如果是消息，就把消息反序列化
                    Message message = JSON.parseObject(line, Message.class);
                    //message from this -> target
                    server.sendMessage(this, message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.clientOffline(this);
        }
    }

    private boolean isNotOnlineYet() {
        return clientName == null;
    }

    public void sendMessage(String message) throws IOException {
        socket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write('\n');
        //清空缓存区
        socket.getOutputStream().flush();
    }
}
