import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class ChatClient
{
    /* UI vars */
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    /* -- UI vars */

    
    private String serverIP;
    private int server_port;
    private Socket client_soc;

    private boolean pending_join;
    private boolean pending_leave;
    private boolean inside;
    
    /* Append message in chat area */
    public void printMessage(final String message)
    {
        chatArea.append(message);
    }

    
    public ChatClient(String server_name, int server_port) throws Exception
    {
	// build UI
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener()
	{
            @Override
            public void actionPerformed(ActionEvent e)
	    {
                try {
		    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });

	// save server IP (addr from DNS name) and port
	serverIP = (InetAddress.getByName(server_name)).getHostAddress();
	this.server_port = server_port;

	pending_join = pending_leave = inside = false;
    }

    
    /* action performed after user input in text box */
    public void newMessage(String message) throws IOException
    {
	message = message.trim();
	String cmd = message.split(" ")[0];
	
	// is client inside a room ?
	if(inside)
	{
	    if(!cmd.matches("^/(join|nick|leave|bye)$") && cmd.startsWith("/"))
		message = "/" + message; // escape normal message starting with /
	}
	else if(cmd.equals("/join"))
	    pending_join = true;
	else if(cmd.equals("/leave"))
	    pending_leave = true;

	System.out.println("TRYING TO SEND: " + message);
	
	DataOutputStream outToserver = new DataOutputStream(client_soc.getOutputStream());
	outToserver.writeBytes(message + "\n");
    }

    
    /* object's main method */
    public void run() throws Exception
    {
	client_soc = new Socket(serverIP,server_port);
	new Thread(new HSocListener()).start();
    }    


    private class HSocListener implements Runnable
    {	
	public HSocListener() { }

	public void run()
	{
	    try
	    {
		BufferedReader inFromServer;
		boolean alive = true;
		
		while(alive)
		{
		    inFromServer = new BufferedReader(new InputStreamReader(client_soc.getInputStream()));
		    String msg = inFromServer.readLine() + "\n"; // readLine() removes end of line

		    printMessage(msg);
		    
		    if(pending_join && msg.equals(ChatServer.ANS_OK))
		    {
			inside = true;
			pending_join = false;
		    }
		    else if(pending_leave && msg.equals(ChatServer.ANS_OK))
		    {
			inside = false;
			pending_leave = false;
		    }
		    else if(msg.equals(ChatServer.ANS_BYE))
			alive = false;
		}

		client_soc.close();
		// System.exit(0);
	    }
	    catch(Exception e)
	    {
		e.printStackTrace();
	    }
	}	
    }
    
    
    /* create client and start its execution */
    public static void main(String[] args) throws Exception
    {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
