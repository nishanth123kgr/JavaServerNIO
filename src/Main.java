public class Main {

    public static void main(String[] args) {
        try {
            System.out.println("Starting server on port 6060...");
            Server server = new Server(6060, 5);
            server.start();
            System.out.println("Server started successfully!");
        } catch (Exception e) {
            System.err.println("Error starting server:");
            e.printStackTrace();
        }
    }
}
