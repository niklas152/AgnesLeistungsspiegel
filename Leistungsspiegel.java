import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Leistungsspiegel {
    private static final Pattern ABSCHLUSS_LINE = Pattern.compile(
            "Abschluss: \\[(\\d+)\\] ([a-zA-Z ]+)"
    );

    private List<Abschluss> abschlüsse;

    Leistungsspiegel(String rawLeistungsspiegelHtml) {
        abschlüsse = new ArrayList<>();

        String preparedLS = prepareLeistungsspiegel(rawLeistungsspiegelHtml);
        System.out.println(preparedLS);
        //Parse(preparedLS);
    }

    private void Parse(String preparedLS) {
        String[] lines = preparedLS.split("\n");

        int parserAtLineIndex = 0;

        while (parserAtLineIndex < lines.length) {
            if (lines[parserAtLineIndex].startsWith("Abschluss")) {
                Abschluss abschluss = new Abschluss();

                abschlüsse.add(new Abschluss());
            }
        }
    }

    private String prepareLeistungsspiegel(String rawLS) {
        String[] leistungsspiegelTable =
                rawLS.substring(
                        rawLS.indexOf("<table"),
                        rawLS.indexOf("</table>") + 8
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

        String leistungspiegel = nicerLS.toString();

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

        // replace Agnes own placeholders with ours
        leistungspiegel = leistungspiegel.replaceAll("&nbsp;\\|&nbsp;", " --|-- ");

        // all that replacing leaves some strange and useless patterns, remove those too
        leistungspiegel = leistungspiegel.replaceAll("\\-\\-\\|\\-\\- \\| \\-\\-\\|\\-\\-", "--|--");
        leistungspiegel = leistungspiegel.replaceAll("\\-\\-\\|\\-\\-  \\-\\-\\|\\-\\-  \\-\\-\\|\\-\\-", "--|--");

        // add line breaks after each table row
        leistungspiegel = leistungspiegel.replace("</tr>", "\n");

        // at this point, only our delimiters should be left - let's replace them with something a little shortter/nicer
        leistungspiegel = leistungspiegel.replaceAll("\\-\\-\\|\\-\\-", "%");

        // now, re-trim and also remove the first and the last two lines since they are irrelevant to parsing
        nicerLS = new StringBuilder();
        leistungsspiegelTable = leistungspiegel.split("\n");

        for (int i = 1; i < leistungsspiegelTable.length - 2; ++i) {
            nicerLS.append(leistungsspiegelTable[i].trim() + "\n");
        }

        return nicerLS.toString();
    }
}
