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
        String leistungsspiegelPage = GetPage(url, cookie);
        return leistungsspiegelPage.substring(
                leistungsspiegelPage.indexOf("<table"),
                leistungsspiegelPage.indexOf("</table>") + 8
        );
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