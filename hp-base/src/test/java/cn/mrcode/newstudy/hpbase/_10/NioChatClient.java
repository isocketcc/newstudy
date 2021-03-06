package cn.mrcode.newstudy.hpbase._10;

import com.alibaba.fastjson.JSON;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * @author : zhuqiang
 * @version : V1.0
 * @date : 2018/5/22 22:08
 */
public class NioChatClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        new NioChatClient().init(6000);
    }

    private Selector selector;
    private Charset charset = Charset.forName("utf-8");
    private CountDownLatch login = new CountDownLatch(1);
    private String userName = "";

    public void init(int port) throws IOException, InterruptedException {
        selector = Selector.open();
        InetSocketAddress isa = new InetSocketAddress(port);
        SocketChannel sc = SocketChannel.open(isa);
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);

        readable();
        // 必须登录成功之后 才能进行说话
        login.await();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line == null || line.isEmpty()) {
                continue;
            }
            ChatRequest chatRequest = new ChatRequest();
            if (line.startsWith("@")) {
                chatRequest.setType(ChatRequest.TYPE_PRIVATE);
                int userSp = line.indexOf(" ");
                if (userSp != -1) {
                    chatRequest.setTo(line.substring(1, userSp));
                    chatRequest.setInfo(line.substring(userSp + 1));
                    chatRequest.setFrom(userName);
                } else {
                    System.err.println("@用户名 信息 请按照此格式发送信息");
                    continue;
                }
            } else {
                chatRequest.setType(ChatRequest.TYPE_ROOM);
                chatRequest.setInfo(line);
                chatRequest.setFrom(userName);
            }
            ByteBuffer encode = charset.encode(JSON.toJSONString(chatRequest));
            sc.write(encode);
        }
    }

    private void readable() throws IOException {
        new Thread(() -> {
            try {
                while (selector.select() > 0) {
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey sk = it.next();
                        it.remove();
                        if (sk.isReadable()) {
                            SocketChannel sc = (SocketChannel) sk.channel();
                            ChatRespones chatRespones = getChatRespones(sc);
                            byte type = chatRespones.getType();
                            switch (type) {
                                case ChatRequest.TYPE_LOGIN:
                                    doLogin(chatRespones, sc);
                                    break;
                                case ChatRequest.TYPE_ROOM:
                                    doRoom(chatRespones);
                                    break;
                                case ChatRequest.TYPE_PRIVATE:
                                    doPrivate(chatRespones);
                                    break;
                            }
                            sk.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

    }

    private ChatRespones getChatRespones(SocketChannel sc) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (sc.read(buffer) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                baos.write(buffer.get());
            }
            buffer.clear();
        }
        CharBuffer decode = charset.decode(ByteBuffer.wrap(baos.toByteArray()));
        return JSON.parseObject(decode.toString(), ChatRespones.class);
    }

    // 处理登录相关逻辑
    private void doLogin(ChatRespones chatRespones, SocketChannel sc) throws IOException {
        if (chatRespones.isSuccess()) {
            login.countDown();
            // 登录成功
            System.out.println(chatRespones.getFrom() + " : " + chatRespones.getInfo());
            return;
        }
        System.out.println(chatRespones.getError());
        // 登录失败的情况下需要继续登录
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            ChatRequest chatRequest = new ChatRequest(ChatRequest.TYPE_LOGIN);
            chatRequest.setUser(line);
            userName = line;
            sc.write(charset.encode(JSON.toJSONString(chatRequest)));
            break;
        }
    }

    // 处理聊天室类型的信息
    private void doRoom(ChatRespones chatRespones) {
        boolean success = chatRespones.isSuccess();
        String info = "";
        if (success) {
            info = chatRespones.getInfo();
        } else {
            info = chatRespones.getError();
        }
        System.out.println("[聊天室]" + chatRespones.getFrom() + " : " + info);
    }

    // 处理私聊的信息
    private void doPrivate(ChatRespones chatRespones) {
        boolean success = chatRespones.isSuccess();
        String info = "";
        if (success) {
            info = chatRespones.getInfo();
        } else {
            info = chatRespones.getError();
        }
        System.out.println("[私聊]" + chatRespones.getFrom() + " : " + info);
    }
}
