import java.io.Console;
import java.util.Scanner;

class UI {
    private Console console;

    private boolean legacyMode = true;
    private Scanner legacyScanner;

    UI() {
        console = System.console();

        if (console == null) {
            Log("WARNING: Failed to init console - fall-back to standard in-/output (i.e. among others things your password won't be hidden when typing on the prompt - be careful)\n");
            legacyScanner = new Scanner(System.in);
        } else {
            // init off console worked
            legacyMode = false;
        }
    }

    void Log(String msg) {
        Log(msg, true);
    }

    void Log(String msg, boolean newLine) {
        if (legacyMode) {
            System.out.print(msg + (newLine ? "\n" : ""));
        } else {
            console.writer().print(msg + (newLine ? "\n" : ""));
            console.flush();
        }
    }

    String getLine(String query) {
        return getLine(query, null, false);
    }

    String getLine(String query, boolean isPassword) {
        return getLine(query, null, isPassword);
    }

    String getLine(String query, String defaultValue, boolean isPassword) {
        StringBuilder input = new StringBuilder();

        if (defaultValue != null) {
            Log(query + "[" + defaultValue + "] > ", false);
        } else {
            Log(query + " > ", false);
        }

        if (isPassword && !legacyMode) {
            for (char c : console.readPassword()) {
                input.append(c);
            }
        } else if (!legacyMode) {
            input.append(console.readLine());
        } else {
            // legacy mode
            input.append(legacyScanner.nextLine());
        }

        return input.toString();
    }
}
