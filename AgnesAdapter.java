import javax.print.DocFlavor;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AgnesAdapter {
    private static final String AGNES_LOGON_URL = "https://agnes.hu-berlin.de/lupo/rds?state=user&type=1&category=auth.login&re=last&startpage=portal.vm";
    private static final String AGNES_START_URL = "https://agnes.hu-berlin.de/lupo/rds?state=user&type=0";

    AgnesAdapter() {
        // TODO
    }

    private String getDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    AgnesLogonResult signIn(String user, String pass) {
        // login form data
        HashMap<String, String> params = new HashMap<>();
        params.put("username", user);
        params.put("password", pass);
        params.put("submit", "Login");

        byte[] postData = new byte[0];
        try {
            postData = getDataString(params).getBytes( StandardCharsets.UTF_8 );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int postDataLength = postData.length;

        URL url = null;

        try {
            url = new URL( AGNES_LOGON_URL );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString( postDataLength ));
        conn.setUseCaches(false);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write( postData );
        } catch (IOException e) {
            e.printStackTrace();
        }

        AgnesLogonResult result = new AgnesLogonResult();

        try {
            result.responseCode = conn.getResponseCode();
            result.responseMessage = conn.getResponseMessage();

            if (result.responseCode == 302 && conn.getHeaderFields().containsKey("Set-Cookie")) {
                result.cookie = conn.getHeaderFields().get("Set-Cookie").get(0);
                result.loggedOn = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    String getLeistungsspiegelLinkFromStartPage(String cookie) {
        String leistungsSpiegelLink = GetLineFromResult(AGNES_START_URL, "Leistungsspiegel", cookie);

        return leistungsSpiegelLink.substring(leistungsSpiegelLink.lastIndexOf("<a href=\"") + 9,
                leistungsSpiegelLink.lastIndexOf("\" class")).replace("&amp;", "&");
    }

    String getLeistungspiegelDetailLinkFromLeistungspiegelPage(String url, String cookie) {
        String leistungsSpiegelDetails = GetLineFromResult(url,">Detailansicht<", cookie);

        return leistungsSpiegelDetails.substring(leistungsSpiegelDetails.lastIndexOf("<a href=\"") + 9,
                leistungsSpiegelDetails.lastIndexOf("\" title")).replace("&amp;", "&");
    }

    String getRawLeistungsspiegel(String url, String cookie) {
        String leistungspiegel = GetPage(url, cookie);

        String[] leistungsspiegelTable =
                leistungspiegel.substring(
                        leistungspiegel.indexOf("<table"),
                        leistungspiegel.indexOf("</table>") + 8
                ).split("\n");

        StringBuilder nicerLS = new StringBuilder();

        for (int i = 0; i < leistungsspiegelTable.length; ++i) {
            // get current line, but without unneccassary whitspace
            String newLine = leistungsspiegelTable[i].trim();

            // remove tablerow start tags
            newLine = newLine.replace("<tr>", "");
            // add new lines instead of tablerow end tags
            newLine = newLine.replace("</tr>", "\n");

            // remove blank lines
            if (!newLine.isEmpty()) {
                nicerLS.append(newLine);
            }
        }

        leistungspiegel = nicerLS.toString();

        // remove html table tags, insert custom seperators (--|--) instead if in middle of row
        leistungspiegel = leistungspiegel.replaceAll("<\\/td><td[a-zA-Z0-9 _=:;\\\"\\-]*>", " --|-- ");
        leistungspiegel = leistungspiegel.replaceAll("<\\/th><th[a-zA-Z0-9 _=:;\\\"\\-]*>", " --|-- ");

        // remove them from start..
        leistungspiegel = leistungspiegel.replaceAll("<td[a-zA-Z0-9 _=:;\\\"\\-]*>", "");
        leistungspiegel = leistungspiegel.replaceAll("<th[a-zA-Z0-9 _=:;\\\"\\-]*>", "");
        // ..and end of line
        leistungspiegel = leistungspiegel.replaceAll("<\\/td>", "");
        leistungspiegel = leistungspiegel.replaceAll("<\\/th>", "");

        // remove italics tags in header line
        leistungspiegel = leistungspiegel.replaceAll("<i>Abschluss", "Abschluss");
        leistungspiegel = leistungspiegel.replaceAll("<i>", "--|-- ");
        leistungspiegel = leistungspiegel.replaceAll("<\\/i>", "");

        // replace weird placeholders with ours
        leistungspiegel = leistungspiegel.replaceAll("&nbsp;|&nbsp;", " --|-- ");

        // add line breaks after each table row
        leistungspiegel = leistungspiegel.replace("</tr>", "\n");

        // now, re-trim and also remove the first and the last two lines since they are irrelevant to parsing
        nicerLS = new StringBuilder();
        leistungsspiegelTable = leistungspiegel.split("\n");
        
        for (int i = 1; i < leistungsspiegelTable.length - 2; ++i) {
            nicerLS.append(leistungsspiegelTable[i].trim() + "\n");
        }

        return nicerLS.toString();
    }

    private String GetLineFromResult(String url, String lineMark, String cookie) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert connection != null;
        connection.setInstanceFollowRedirects(false);
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        connection.setRequestProperty("Cookie", cookie);
        connection.setUseCaches(false);

        InputStream response = null;
        try {
            response = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStreamReader iSR;
        try {
            iSR = new InputStreamReader(response, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }

        int c = 0;
        try {
            assert response != null;
            c = iSR.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder line = new StringBuilder();
        while ( c > 0 ) {
            try {
                c = iSR.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (c == '\n') {
                if (line.toString().contains(lineMark)) {
                    return line.toString();
                }
                line = new StringBuilder();
            }
            else {
                line.append((char) c);
            }
        }
        return line.toString();
    }

    private String GetPage(String url, String cookie) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert connection != null;
        connection.setInstanceFollowRedirects(false);
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        connection.setRequestProperty("Cookie", cookie);
        connection.setUseCaches(false);

        InputStream response = null;
        try {
            response = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStreamReader iSR;
        try {
            iSR = new InputStreamReader(response, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }

        int c = 0;
        try {
            c = iSR.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder line = new StringBuilder();
        while ( c > 0 ) {
            try {
                c = iSR.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            line.append((char) c);
        }
        return line.toString();
    }

    class AgnesLogonResult {
        int responseCode = -1;
        String responseMessage = null;
        boolean loggedOn = false;
        String cookie = null;
    }
}