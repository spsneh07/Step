import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameService {

    // username -> userId
    private final ConcurrentHashMap<String, String> usernameToUserId = new ConcurrentHashMap<>();

    // username -> attempt count
    private final ConcurrentHashMap<String, AtomicInteger> attemptCount = new ConcurrentHashMap<>();

    // Track most attempted username
    private final ConcurrentHashMap<String, AtomicInteger> popularityMap = new ConcurrentHashMap<>();

    // ----------------------------
    // Check Availability
    // ----------------------------
    public boolean checkAvailability(String username) {
        username = normalize(username);

        // Increment attempt count atomically
        attemptCount
                .computeIfAbsent(username, k -> new AtomicInteger(0))
                .incrementAndGet();

        return !usernameToUserId.containsKey(username);
    }

    // ----------------------------
    // Register Username
    // ----------------------------
    public boolean registerUsername(String username, String userId) {
        username = normalize(username);

        // Atomic put if absent (prevents race condition)
        return usernameToUserId.putIfAbsent(username, userId) == null;
    }

    // ----------------------------
    // Suggest Alternatives
    // ----------------------------
    public List<String> suggestAlternatives(String username) {
        username = normalize(username);
        List<String> suggestions = new ArrayList<>();

        int suffix = 1;
        while (suggestions.size() < 3) {
            String candidate = username + suffix;
            if (!usernameToUserId.containsKey(candidate)) {
                suggestions.add(candidate);
            }
            suffix++;
        }

        // Also try replacing underscore with dot
        if (username.contains("_")) {
            String dotVersion = username.replace("_", ".");
            if (!usernameToUserId.containsKey(dotVersion)) {
                suggestions.add(dotVersion);
            }
        }

        return suggestions;
    }

    // ----------------------------
    // Get Most Attempted Username
    // ----------------------------
    public String getMostAttempted() {
        return attemptCount.entrySet()
                .stream()
                .max(Comparator.comparingInt(e -> e.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ----------------------------
    // Normalize Username
    // ----------------------------
    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    // ----------------------------
    // Example Usage
    // ----------------------------
    public static void main(String[] args) {
        UsernameService service = new UsernameService();

        service.registerUsername("john_doe", "user123");

        System.out.println(service.checkAvailability("john_doe"));   // false
        System.out.println(service.checkAvailability("jane_smith")); // true

        System.out.println(service.suggestAlternatives("john_doe"));

        service.checkAvailability("admin");
        service.checkAvailability("admin");
        service.checkAvailability("admin");

        System.out.println("Most attempted: " + service.getMostAttempted());
    }
}
