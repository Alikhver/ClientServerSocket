import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MonoThreadClientHandler implements Runnable {

    private final BusStation busStation;

    private final Socket clientSocket;

    private DataInputStream inputStream;

    private DataOutputStream outputStream;

    public MonoThreadClientHandler(Socket client, BusStation busStation) {
        this.busStation = busStation;
        this.clientSocket = client;
    }

    @Override
    public void run() {
        try {
            this.inputStream = new DataInputStream(clientSocket.getInputStream());
            this.outputStream = new DataOutputStream(clientSocket.getOutputStream());

            List<Trip> trips = busStation.getTrips();
            Trip trip;
            StringBuilder response;
            String request;

            System.out.println("Server reading from channel");

            while (true) {
                response = new StringBuilder().append("Available trips:\n");
                for (int i = 0; i < trips.size(); i++) {
                    response.append(i + 1).append(": ").append(trips.get(i).getData())
                            .append(" : ").append(trips.get(i).getDate().getTime())
                            .append('\n');
                }

                sendResponse(response.append("Choose a trips number: ").toString());
                request = getRequest();

                try {
                    if (Integer.parseInt(request) > 0 && Integer.parseInt(request) <= trips.size()) {
                        trip = busStation.getTrips().get(Integer.parseInt(request) - 1);
                        break;
                    } else {
                        sendWrongInputResponse("There is no trip with entered number: enter for OK");
                    }
                } catch (NumberFormatException e) {
                    sendWrongInputResponse("Wrong data format: enter for OK");
                }
            }

            while (true) {
                response = new StringBuilder()
                        .append("Available seats:").append('\n').append("  ");

                for (int i = 0; i < trip.getSeats()[1].length; i++) {
                    response.append(i).append(" ");
                }
                response.append('\n');

                for (int i = 0; i < trip.getSeats().length; i++) {
                    response.append(i).append(" ");
                    for (int j = 0; j < trip.getSeats()[0].length; j++) {
                        if (trip.getSeats()[i][j] == 1) {
                            response.append("x ");
                        } else {
                            response.append(". ");
                        }
                    }
                    response.append('\n');
                }
                response.append("Choose a seat: (format: r p (r for row, p for place)) ");
                sendResponse(response.toString());

                request = getRequest();

                try {
                    String[] args = request.split(" ");
                    if (args[0].equals("") || args[1].equals("")) {
                        throw new NumberFormatException();
                    }
                    // TODO wrong handling on input "n ", n is number
                    int row = Integer.parseInt(args[0]);
                    int place = Integer.parseInt(args[1]);
                    response = new StringBuilder();
                    if (row >= 0 && row < trip.getSeats()[0].length && place >= 0 && place < trip.getSeats().length) {
                        if (trip.reserve(row, place)) {
                            response.append("You have reserved a place: ")
                                    .append(row).append(" ").append(place)
                                    .append("\nGood Bye!");
                            sendResponse(response.toString());
                            break;
                        } else {
                            sendWrongInputResponse(response.append("This place was already reserved\n")
                                    .append("Choose a seat again:").toString());
                        }
                    } else {
                        sendWrongInputResponse("Incorrect row or seat number: enter for OK");
                    }
                } catch (NumberFormatException e) {
                    sendWrongInputResponse("Wrong data format: enter for OK");
                }
            }

            System.out.println("Client disconnected");
            System.out.println("Closing connections & channels.");

            clientSocket.close();

            System.out.println("Closing connections & channels - DONE.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRequest() throws IOException {
        String request;
        while (true) {
            if (inputStream.available() > 0) {
                request = inputStream.readUTF();

                // if request == exit
                if (isExitMessage(request)) {
                    // TODO do smth if exit message
                    clientSocket.close();

                }
                break;
            }
        }
        return request;
    }

    private void sendResponse(String str) throws IOException {
        outputStream.writeUTF(str);
        outputStream.flush();
    }

    private boolean isExitMessage(String str) {
        if (str.equalsIgnoreCase("quit")) {
            System.out.println("Client initialize connections suicide ...");
            return true;
        }
        return false;
    }

    private void sendWrongInputResponse(String msg) throws IOException {
        sendResponse("Input is not correct: " + msg);
        getRequest();
    }
}