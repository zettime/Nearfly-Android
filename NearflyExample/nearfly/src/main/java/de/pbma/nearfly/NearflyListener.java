package de.pbma.nearfly;

/**
 * Interface which is needed to react to concerns of the nearfly service
 */
public interface NearflyListener {
    void onLogMessage(String output);
    /*void onStateChanged(String state);
    void onRootNodeChanged(String rootNode);*/
    void onMessage(String channel, String message);
    // void onStream(Payload payload);
    // void onBinary(Payload payload);
    void onFile(String channel, String path, String textAttachment);

    /*public interface StatusChange{
        void onLogMessage(CharSequence msg);
        void onStateChanged(String state);
        void onRootNodeChanged(String rootNode);
    }

    public interface Message{
        void onMessage(String channel, String message);
    }

    public interface FileInfMessage{
        void onFile(String path, String textAttachment);
    }*/
}
