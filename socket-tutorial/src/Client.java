import java.io.*;
import java.net.Socket;

public class Client {

    public static void main(String[] args) {
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_SERVER_PORT = 8888;
        Socket socket = null;
        BufferedWriter writer =null;
        final String QUIT = "quit";
        try {
            //创建连接
            socket = new Socket(DEFAULT_SERVER_HOST,DEFAULT_SERVER_PORT);
            //创建IO流
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                String input = consoleReader.readLine();
                // 发信息给服务器
                writer.write(input+"\n");
                writer.flush();
                //读取服务器信息
                String msg = reader.readLine();
                System.out.println(msg);
                if (QUIT.equals(input)){
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (writer!=null){
                try {
                    writer.close();
                    System.out.println("关闭socket");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
