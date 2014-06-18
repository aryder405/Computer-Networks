/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom2;

/*
 * @author Adam Ryder 
 * Multi-Threaded Chat room Server
 * 3.1.2012
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer2 {

    private static final int PORT = 14733;
    private static int count = 0;
    private static HashSet<String> namesAndPass = new HashSet<String>();
    private static HashSet<String> userNames = new HashSet<String>();
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    private static final int MAXCLIENT = 3;
    private static Handler[] clients = new Handler[MAXCLIENT];

    /*
     * Method to check the list of user names and passwords. Returns true if the
     * name and password match, false if not.
     */
    public static synchronized boolean checkLogin(String info, String userNam) {
        System.out.println("Checking names/pass for: " + info);
        if (namesAndPass.contains(info)) {
            System.out.println("name/pass confirmed");
            return true;
        } else {
            System.out.println("login failed");
            return false;
        }
    }

    /*
     * Method to check name n with the list of names currently logged in on
     * server. True if logged in, false if not.
     */
    public static boolean checkLocalNames(String n) {
        System.out.println("Checking user names for: " + n);
        if (userNames.contains(n)) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Method to register a user name and password to the txt file and also
     * updates the names list and the names and passwords list. This method is
     * synchronized so only 1 thread can run the method at a time.
     */
    public static synchronized void registerUser(String n) {
        try {
            FileWriter fstream = new FileWriter("login.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("\n" + n);
            out.close();
            namesAndPass.add(n);
            StringTokenizer str = new StringTokenizer(n);
            String user = str.nextToken();
            userNames.add(user);
            System.out.println("New user register: " + n);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /*
     * Method to send a String to all clients
     */
    public static void sendAll(String z) {
        for (PrintWriter w : writers) {
            w.println(z);
        }
    }

    /*
     * Method to process the message from the client "Send" "Send all" "who" and
     * "logout"
     */
    public static void processMessage(String n, Handler h) {
        StringTokenizer st = new StringTokenizer(n);
        String a = st.nextToken();
        String b;
        //Process a send request
        if (a.equalsIgnoreCase("send")) {
            b = st.nextToken();
            //Processs a send all request
            if (b.equalsIgnoreCase("all")) {
                sendAll(h.name + ": " + n.substring(8));
            } else if (userNames.contains(b)) {
                //Process a send request to a specific client.
                for (int i = 0; i < MAXCLIENT; i++) {
                    if (clients[i].name.equalsIgnoreCase(b)) {
                        clients[i].out.println("Message from " + h.name + ": " + n.substring(5 + clients[i].name.length()));
                        break;
                    }
                }
            }
        }
        //Process a who request.
        if (a.equalsIgnoreCase("who")) {
            System.out.println("Who requested by: " + h.name);
            h.out.println(userNames.toString());
        }
        //Process a logout request.
        if (a.equalsIgnoreCase("logout")) {
            try {
                userNames.remove(h.name);
                writers.remove(h.out);
                count--;
                sendAll(h.name + " has left the room");
                System.out.println(h.name + " has logged out");
                h.socket.close();

            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Server running.");
        ServerSocket listener = new ServerSocket(PORT);
        //populate namesAndPass List with data from login.txt file
        try {
            File file = new File("login.txt");
            Scanner scanFile = new Scanner(file);
            String tmp;
            while (scanFile.hasNextLine()) {
                tmp = scanFile.nextLine();
                namesAndPass.add(tmp);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        //Spawn the handler thread if client count is less than MAXCLIENT
        try {
            while (true) {
                if (count < MAXCLIENT) {
                    new Handler(listener.accept(), count++).start();
                }
            }
        } finally {
            listener.close();
        }
    }

    /**
     * Handlers threads are spawned from the listening loop and are responsible
     * for a dealing with a single client
     */
    private static class Handler extends Thread {

        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int ID;
        private boolean loggedIn = false;

        //Constructor
        public Handler(Socket socket, int ID) {
            this.socket = socket;
            clients[ID] = this;
        }

        /*
         * Main method in the Handler class that deals with the input from the
         * Client.
         */
        public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                /*
                 * This do/while loop will run till the client successfully
                 * logins.
                 */
                out.println("To login type 'login username password' or 'register username password'");
                do {
                    String s = in.readLine().toLowerCase();
                    if (!s.isEmpty()) {
                        StringTokenizer st = new StringTokenizer(s);
                        String command = st.nextToken();
                        if (st.hasMoreTokens()) {
                            String nam = st.nextToken();
                            if (st.hasMoreTokens()) {
                                String pw = st.nextToken();
                                String nameAndPw = nam + " " + pw;
                                System.out.println(name + ": " + command);
                                //Process a Login request.
                                if (command.equalsIgnoreCase("login")) {
                                    if (checkLogin(nameAndPw, nam) == true && checkLocalNames(nam) == false) {
                                        this.loggedIn = true;
                                        this.name = nam;
                                        System.out.println("New user logged in: " + nam);
                                        synchronized (userNames) {
                                            userNames.add(nam);
                                        }
                                        writers.add(out);
                                        out.println("You are now logged in as: " + name);
                                        sendAll(name + " has logged in");
                                        break;
                                    } else {
                                        out.println("Login failed, try again");
                                    }
                                    //process a Register request.
                                } else if (command.equalsIgnoreCase("register")) {
                                    if (!checkLocalNames(nam)) {
                                        registerUser(nameAndPw);
                                        this.loggedIn = true;
                                        this.name = nam;
                                        writers.add(out);
                                        out.println("You are now logged in as: " + name);
                                        sendAll(name + " has logged in");
                                        break;
                                    } else {
                                        out.println("Name already in use");
                                    }
                                }
                            }
                        }
                    }
                } while (!loggedIn);
                /*
                 * Once logged in, this loop processes all input from the
                 * client.
                 */
                out.println(" type 'send' 'send all' 'who' or 'logout'");
                while (loggedIn) {
                    String tmp = in.readLine();
                    if (!tmp.isEmpty()) {
                        System.out.println(name + ": " + tmp);
                        processMessage(tmp, this);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
