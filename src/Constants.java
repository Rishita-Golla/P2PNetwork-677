import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Constants {

    public static final List<String> POSSIBLE_ITEMS = Arrays.asList("fish","salt","boar");
    public static final long MAX_TIMEOUT = 4000;
    public static final int MAX_HOP = 3;
    public static final int MAX_ITEM_COUNT = 4;
    public static final Map<String, Integer> SELLER_PURCHASE_PRICES = Map.of("fish", 10, "salt", 20, "boar",30 );

}
