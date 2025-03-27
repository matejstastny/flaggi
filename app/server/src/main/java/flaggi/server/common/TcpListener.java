/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.common;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import flaggi.proto.ClientMessages.ClientMessageWrapper;
import flaggi.server.client.UserHandler;
import flaggi.server.client.User;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

public class TcpListener implements Runnable {

    private final int port;
    private final BlockingQueue<ClientMessageWrapper> messageQueue;
    private final Map<String, User> users;

    // Constructor --------------------------------------------------------------

    public TcpListener(int port, BlockingQueue<ClientMessageWrapper> messageQueue, Map<String, User> users) {
        this.port = port;
        this.messageQueue = messageQueue;
        this.users = users;
    }

    // Accesors -----------------------------------------------------------------

    public BlockingQueue<ClientMessageWrapper> getMessageQueue() {
        return messageQueue;
    }

    // Update -------------------------------------------------------------------

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new UserHandler(clientSocket, messageQueue, users), "Client Handler Thread").start();
            }
        } catch (Exception e) {
            Logger.log(LogLevel.ERROR, "An error occurred in the TCP listener.", e);
        }
    }
}
