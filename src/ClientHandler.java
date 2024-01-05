import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientHandler implements Runnable{

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket){
        try{
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = bufferedReader.readLine();
            boolean name_exists = false;
            for (ClientHandler clientHandler : clientHandlers) {
                if (clientHandler.clientUsername.equals(clientUsername)) {
                    name_exists = true;
                }
            }
            if (!name_exists){
                clientHandlers.add(this);
            }


            broadcastMessage("SERVER : " + clientUsername + " has joined the chat!");

        }catch(IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run(){
        String messageFromClient;

        while(socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                String[] split_message = messageFromClient.split(" ");      // THINK A BETTER METHOD TO DO THIS
                if (split_message[2].equals("/w")){

                    String sender_username = split_message[0];
                    String recipient_username = split_message[3];
                    String message = String.join(" ", Arrays.stream(split_message, 4, split_message.length).toArray(String[]::new));
                    // FACEPALM, FIND ANOTHER SOLUTION TO THIS
                    whisperMessage(message, sender_username, recipient_username);
                }
                else{
                    broadcastMessage(messageFromClient);
                }

            }catch(IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend){         // NEEDS OPTIMIZATION
        for (ClientHandler clientHandler : clientHandlers){
            try{
                if (!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }catch(IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void whisperMessage(String messageToSend, String sender_username, String recipient_username) throws IOException {
        boolean userFound = false;
        for (ClientHandler clientHandler : clientHandlers){
            try{
                if (clientHandler.clientUsername.equals(recipient_username)){
                    userFound = true;
                    clientHandler.bufferedWriter.write(sender_username + "(whisper) : " + messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }catch(IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
        if (!userFound){
            bufferedWriter.write(recipient_username + " does not exist in the chat");
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
    }
    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("SERVER : " + clientUsername + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
        try{
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(socket != null){
                socket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
