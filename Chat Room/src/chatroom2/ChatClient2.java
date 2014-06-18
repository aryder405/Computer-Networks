/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom2;

/*
 * @author Adam Ryder 
 * Multi-Threaded Chat room Client 
 * 3.1.2012
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Scanner;
import java.net.Socket;
import javax.swing.*;

public class ChatClient2 extends JFrame {

    BufferedReader inFromServer, userEntry;
    PrintWriter outToServer;
    JTextField textField = new JTextField();
    JTextArea messageArea = new JTextArea();

    /*
     * Client Constructor sets all GUI fields.
     */
    public ChatClient2() {

        // Layout GUI
        super("Some random Chat Room");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textField.setEditable(true);
        messageArea.setEditable(false);
        this.setLayout(new BorderLayout());
        add(textField, BorderLayout.SOUTH);
        add(new JScrollPane(messageArea), BorderLayout.CENTER);
        setVisible(true);
        try {
            //Connect the client to the socket and enable data streams
            Socket socket = new Socket("localHost", 14733);
            inFromServer = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            outToServer = new PrintWriter(socket.getOutputStream(), true);

        } catch (Exception e) {
            System.out.println(e);
        }

        /*
         * Add listener to textField and send all input
         * to the server, till "logout" is received.
         */
        textField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                outToServer.println(textField.getText());
                if (textField.getText().equalsIgnoreCase("logout")) {
                    System.exit(1);
                }
                textField.setText("");
            }
        });

        try {
            //Loop to process all messages from server
            while (true) {

                String line = inFromServer.readLine();
                messageArea.append(line + "\n");
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            }
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient2 client = new ChatClient2();
    }
}
