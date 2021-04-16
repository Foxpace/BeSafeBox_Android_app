package motionapps.besafebox.tools;

import java.io.IOException;

public class InternetTools {

    /**
     * Simple method to check internet connection
     * @return boolean - if it is connected to internet
     */
    public static boolean isConnected() {
        try {
            String command = "ping -c 1 google.com";
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
