package com.kiko.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpanishSceneDescription {
    private static final int MAX_ITEM_TYPES = 3;
    private static final float MIN_CONFIDENCE = 0.40f;
    private static final Map<String, SpanishNoun> SPANISH_LABELS = createTranslations();

    private SpanishSceneDescription() {
    }

    public static String describe(List<SceneLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return "No logro distinguir qué hay delante de mí.";
        }

        List<SceneLabel> sorted = new ArrayList<>(labels);
        sorted.sort((left, right) ->
                Float.compare(right.getConfidence(), left.getConfidence()));

        Map<String, CountedItem> visibleItems = new LinkedHashMap<>();
        for (SceneLabel label : sorted) {
            if (label == null
                    || label.getText() == null
                    || label.getConfidence() < MIN_CONFIDENCE) {
                continue;
            }

            String key = label.getText().trim().toLowerCase(Locale.ROOT);
            SpanishNoun noun = SPANISH_LABELS.get(key);
            if (noun == null) {
                continue;
            }

            CountedItem existing = visibleItems.get(key);
            if (existing != null) {
                existing.count++;
            } else if (visibleItems.size() < MAX_ITEM_TYPES) {
                visibleItems.put(key, new CountedItem(noun));
            }
        }

        if (visibleItems.isEmpty()) {
            return "Veo algo, pero todavía no sé describirlo bien.";
        }

        List<String> phrases = new ArrayList<>();
        for (CountedItem item : visibleItems.values()) {
            phrases.add(item.describe());
        }
        return "Veo " + join(phrases) + ".";
    }

    private static String join(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        if (items.size() == 2) {
            return items.get(0) + " y " + items.get(1);
        }
        return items.get(0) + ", " + items.get(1) + " y " + items.get(2);
    }

    private static Map<String, SpanishNoun> createTranslations() {
        Map<String, SpanishNoun> translations = new HashMap<>();
        put(translations, "person", "una persona", "personas");
        put(translations, "bicycle", "una bicicleta", "bicicletas");
        put(translations, "car", "un auto", "autos");
        put(translations, "motorcycle", "una motocicleta", "motocicletas");
        put(translations, "airplane", "un avión", "aviones");
        put(translations, "bus", "un autobús", "autobuses");
        put(translations, "train", "un tren", "trenes");
        put(translations, "truck", "un camión", "camiones");
        put(translations, "boat", "un barco", "barcos");
        put(translations, "traffic light", "un semáforo", "semáforos");
        put(translations, "fire hydrant", "un hidrante", "hidrantes");
        put(translations, "stop sign", "una señal de alto", "señales de alto");
        put(translations, "parking meter", "un parquímetro", "parquímetros");
        put(translations, "bench", "un banco", "bancos");
        put(translations, "bird", "un pájaro", "pájaros");
        put(translations, "cat", "un gato", "gatos");
        put(translations, "dog", "un perro", "perros");
        put(translations, "horse", "un caballo", "caballos");
        put(translations, "sheep", "una oveja", "ovejas");
        put(translations, "cow", "una vaca", "vacas");
        put(translations, "elephant", "un elefante", "elefantes");
        put(translations, "bear", "un oso", "osos");
        put(translations, "zebra", "una cebra", "cebras");
        put(translations, "giraffe", "una jirafa", "jirafas");
        put(translations, "backpack", "una mochila", "mochilas");
        put(translations, "umbrella", "un paraguas", "paraguas");
        put(translations, "handbag", "un bolso", "bolsos");
        put(translations, "tie", "una corbata", "corbatas");
        put(translations, "suitcase", "una maleta", "maletas");
        put(translations, "frisbee", "un disco", "discos");
        put(translations, "skis", "un par de esquís", "pares de esquís");
        put(translations, "snowboard", "una tabla de nieve", "tablas de nieve");
        put(translations, "sports ball", "una pelota", "pelotas");
        put(translations, "kite", "una cometa", "cometas");
        put(translations, "baseball bat", "un bate", "bates");
        put(translations, "baseball glove", "un guante", "guantes");
        put(translations, "skateboard", "una patineta", "patinetas");
        put(translations, "surfboard", "una tabla de surf", "tablas de surf");
        put(translations, "tennis racket", "una raqueta", "raquetas");
        put(translations, "bottle", "una botella", "botellas");
        put(translations, "wine glass", "una copa", "copas");
        put(translations, "cup", "una taza", "tazas");
        put(translations, "fork", "un tenedor", "tenedores");
        put(translations, "knife", "un cuchillo", "cuchillos");
        put(translations, "spoon", "una cuchara", "cucharas");
        put(translations, "bowl", "un tazón", "tazones");
        put(translations, "banana", "una banana", "bananas");
        put(translations, "apple", "una manzana", "manzanas");
        put(translations, "sandwich", "un sándwich", "sándwiches");
        put(translations, "orange", "una naranja", "naranjas");
        put(translations, "broccoli", "un brócoli", "brócolis");
        put(translations, "carrot", "una zanahoria", "zanahorias");
        put(translations, "hot dog", "un pancho", "panchos");
        put(translations, "pizza", "una pizza", "pizzas");
        put(translations, "donut", "una dona", "donas");
        put(translations, "cake", "una torta", "tortas");
        put(translations, "chair", "una silla", "sillas");
        put(translations, "couch", "un sofá", "sofás");
        put(translations, "potted plant", "una planta", "plantas");
        put(translations, "bed", "una cama", "camas");
        put(translations, "dining table", "una mesa", "mesas");
        put(translations, "toilet", "un inodoro", "inodoros");
        put(translations, "tv", "un televisor", "televisores");
        put(translations, "laptop", "una computadora", "computadoras");
        put(translations, "mouse", "un ratón", "ratones");
        put(translations, "remote", "un control remoto", "controles remotos");
        put(translations, "keyboard", "un teclado", "teclados");
        put(translations, "cell phone", "un teléfono", "teléfonos");
        put(translations, "microwave", "un microondas", "microondas");
        put(translations, "oven", "un horno", "hornos");
        put(translations, "toaster", "una tostadora", "tostadoras");
        put(translations, "sink", "un fregadero", "fregaderos");
        put(translations, "refrigerator", "un refrigerador", "refrigeradores");
        put(translations, "book", "un libro", "libros");
        put(translations, "clock", "un reloj", "relojes");
        put(translations, "vase", "un florero", "floreros");
        put(translations, "scissors", "unas tijeras", "tijeras");
        put(translations, "teddy bear", "un oso de peluche", "osos de peluche");
        put(translations, "hair drier", "un secador", "secadores");
        put(translations, "toothbrush", "un cepillo de dientes", "cepillos de dientes");
        return Collections.unmodifiableMap(translations);
    }

    private static void put(
            Map<String, SpanishNoun> translations,
            String english,
            String singular,
            String plural
    ) {
        translations.put(english, new SpanishNoun(singular, plural));
    }

    private static final class SpanishNoun {
        private final String singular;
        private final String plural;

        private SpanishNoun(String singular, String plural) {
            this.singular = singular;
            this.plural = plural;
        }
    }

    private static final class CountedItem {
        private final SpanishNoun noun;
        private int count = 1;

        private CountedItem(SpanishNoun noun) {
            this.noun = noun;
        }

        private String describe() {
            return count == 1 ? noun.singular : count + " " + noun.plural;
        }
    }
}
