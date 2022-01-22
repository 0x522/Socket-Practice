import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("请输入你的昵称:");
        Scanner sc = new Scanner(System.in);
        String name = sc.nextLine();

        Socket socket = new Socket("127.0.0.1", 8080);
        //将昵称发送给服务器
        socket.getOutputStream().write(name.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write('\n');
        socket.getOutputStream().flush();
        System.out.println("连接成功!");

        new Thread(() -> readFromServer(socket)).start();

        while (true) {
            System.out.println("输入你要发送的聊天消息");
            System.out.println("格式是id:message,例如,1:hello 代表向id为1的用户发送hello消息");
            System.out.println("id=0代表向所有用户发送消息");

            String line = sc.nextLine();
            if (!line.contains(":")) {
                System.err.println("输入的格式不对");
            } else {
                int index = line.indexOf(':');
                int id = Integer.parseInt(line.substring(0, index));
                String message = line.substring(index + 1);

                String json = JSON.toJSONString(new Message(id, message));
                //发送给服务器
                socket.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().write('\n');
                socket.getOutputStream().flush();
            }
        }
    }

    public static void readFromServer(Socket socket) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                //读到消息不是null，向客户端发送
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
