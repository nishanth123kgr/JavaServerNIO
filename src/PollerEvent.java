public class PollerEvent {

    SocketWrapper socketWrapper;
    int interestOps;

    public PollerEvent(SocketWrapper socketWrapper, int interestOps) {
        this.reset(socketWrapper, interestOps);
    }

    public void reset(SocketWrapper socketWrapper, int interestOps) {
        this.socketWrapper = socketWrapper;
        this.interestOps = interestOps;
    }

    public void reset() {
        this.reset(null, 0);
    }

    @Override
    public String toString() {
        return "PollerEvent " + socketWrapper + " interestOps " + interestOps;
    }
}
