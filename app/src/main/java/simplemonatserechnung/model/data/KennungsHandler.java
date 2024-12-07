package simplemonatserechnung.model.data;

import java.util.HashMap;
import java.util.List;

public class KennungsHandler<T extends Kennung> {
    private HashMap<String, T> khash;

    public KennungsHandler(List<T> liste) {
        scanlist(liste);
    };

    public T getEintrag(String kennung) {
        return khash.get(kennung);
    }

    public void scanlist(List<T> liste) {
        khash = new HashMap<String, T>();
        for (T item : liste) {
            String kennung = item.getKennung();
            if (khash.get(kennung) == null) {
                khash.put(kennung, item);
            } else {
                throw new IllegalArgumentException("Kennung '" + kennung + "' bereits vergeben");
            }
        }
    }

    public String printlist(String prefix) {
        StringBuilder str = new StringBuilder();
        str.append(prefix);
        for (String kennung : khash.keySet()) {
            str.append(prefix + ": " + kennung);
        }
        return str.toString();
    }
}
