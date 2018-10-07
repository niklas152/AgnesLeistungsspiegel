import java.util.List;
import java.util.Map;

public class Abschluss {
    class Studiengang {
        String nummer;
        String name;
    }

    int nummer;
    String typ; // eg: bachelor of science
    Studiengang studiengang;
    String fachkennzeichen; // eg: M - Monobachelor
    String prüfungsordnungsversion;

    int modulpunkteGesamt;
    int modulpunkteÜberfachlicherWahlpflichtbereichGesamt;

    Map<String, Modul> überfachlicheWahlpflichtbereiche;

    List<Modul> pflichtbereich;
}
