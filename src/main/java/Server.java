import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Server {
    private static AtomicInteger COUNTER = new AtomicInteger();

    private final ServerSocket server;
    /**
     * 存放客户端用户信息
     * {id:用户名}
     */
    private final Map<Integer, ClientConnection> clients = new ConcurrentHashMap<>();

    //TCP onnections : 0 ~ 65535
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public void start() throws IOException {
        while (true) {
            Socket socket = server.accept();
            new ClientConnection(COUNTER.incrementAndGet(), socket, this).start();
        }
    }

    public static void main(String[] args) throws IOException {
        new Server(8080).start();
    }

    public String getAllClientInfo() {
        return clients.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().getClientName()).collect(Collectors.joining(","));
    }

    /**
     * 注册
     * 向并发容器内写入k-v模型
     *
     * @param clientConnection
     */
    public void registerClient(ClientConnection clientConnection) {
        //example : {1:connection1}
        clients.put(clientConnection.getClientId(), clientConnection);
        this.clientOnline(clientConnection);
    }

    /**
     * 上线通知
     *
     * @param clientWhoHasJustLogged
     */
    private void clientOnline(ClientConnection clientWhoHasJustLogged) {
        clients.values().forEach(client -> {
            dispatchMessage(client, "系统", "所有人", clientWhoHasJustLogged.getClientName() + "上线了！" + getAllClientInfo());
        });
    }

    /**
     * 下线通知
     *
     * @param clientConnection
     */
    public void clientOffline(ClientConnection clientConnection) {
        clients.remove(clientConnection.getClientId());
        clients.values().forEach(client -> {
            dispatchMessage(client, "系统", "所有人", clientConnection.getClientName() + "下线了!" + getAllClientInfo());
        });
    }

    /**
     * 消息分发模板
     *
     * @param client  连接对象
     * @param src     发送消息的用户
     * @param target  接受消息的用户
     * @param message 消息内容
     */
    public void dispatchMessage(ClientConnection client, String src, String target, String message) {
        try {
            client.sendMessage(src + "对" + target + "说" + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送消息类型
     * 1.广播 当 message id = 0
     * 2.点对点
     *
     * @param src
     * @param message
     * @throws IOException
     */
    public void sendMessage(ClientConnection src, Message message) throws IOException {
        if (message.getId() == 0) {
            //如果 id = 0 就属于广播消息
            clients.values().forEach(client -> {
                dispatchMessage(client, src.getClientName(), "所有人", message.getMessage());
            });
        } else {
            //否则属于点对点消息
            int targetUser = message.getId();
            ClientConnection target = clients.get(targetUser);
            if (target == null) {
                System.err.println("用户" + targetUser + "不存在");
            } else {
                dispatchMessage(target, src.getClientName(), "你", message.getMessage());
            }
        }
    }

}


