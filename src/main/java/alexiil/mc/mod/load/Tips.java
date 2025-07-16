package alexiil.mc.mod.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import javax.annotation.Nullable;

/** Basic tips manager. Provides access to a single list of tips. */
public class Tips {

    private static String language = "en_us";
    private static final List<String> tips = new ArrayList<>();
    private static boolean anyTips = false;

    static {
        // Just ensure that nothing can crash by having an empty list
        tips.add("Tips haven't been loaded yet!");
    }

    public static void load() {
        File options = new File("options.txt");
        if (options.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(options))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length > 1 && parts[0].equals("lang")) {
                        language = parts[1].toLowerCase(Locale.ROOT);
                        break;
                    }
                }
            } catch (IOException io) {
                CLSLog.warn("Failed to load language from options.txt", io);
            }
        }

        CLSLog.info("Target tips language: " + language);

        File f = new File("config/customloadingscreen_tips.txt");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            load(parseTips(br));
        } catch (FileNotFoundException e) {
            CLSLog.info("No tip file found at " + f);
        } catch (IOException e) {
            CLSLog.warn("Failed to load the tips file: " + f, e);
        }
    }

    /** Clears out the current list of tips and sets it to the given {@link List}.
     * <p>
     * This will {@link Collections#shuffle(List)} the tips list or add a default tip if none were loaded. */
    public static void load(List<String> from) {
        tips.clear();
        tips.addAll(from);
        Collections.shuffle(tips);
        if (tips.isEmpty()) {
            tips.add("Tips file was empty!");
            anyTips = false;
        } else {
            anyTips = true;
        }
    }

    public static List<String> parseTips(BufferedReader from) throws IOException {
        List<String> lines = new ArrayList<>();

        boolean firstTipFlag = false;
        boolean isFirstTipLangIndicator = false;
        String _line;
        while ((_line = from.readLine()) != null) {
            _line = _line.trim();
            if (_line.isEmpty() || _line.startsWith("#")) {
                // Comment
            } else {
                if (!firstTipFlag) {
                    firstTipFlag = true;
                    isFirstTipLangIndicator = _line.startsWith("[lang:") && _line.endsWith("]");
                }
                lines.add(_line);
            }
        }

        List<List<String>> availableLangs = new ArrayList<>();
        for (String line: lines) {
            if (line.startsWith("[lang:") && line.endsWith("]")) {
                String langArg = line.substring(6, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                String[] args = langArg.split(",");
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
                availableLangs.add(Arrays.asList(args));
            }
        }

        // Target lang not found edge case
        boolean foundTargetLang = false;
        String targetLang = language;
        for (List<String> langs: availableLangs) {
            if (langs.contains(targetLang)) {
                foundTargetLang = true;
                break;
            }
        }
        if (!foundTargetLang) {
            // Cuz we treat the first section as en_us section
            boolean foundEN_US = !isFirstTipLangIndicator;
            if (!foundEN_US) {
                for (List<String> langs: availableLangs) {
                    if (langs.contains("en_us")) {
                        foundEN_US = true;
                        break;
                    }
                }
            }
            // Falls back to en_us if possible
            // Otherwise use the first language
            targetLang = foundEN_US ? "en_us" : availableLangs.get(0).get(0);
        }

        // Treat the first section as en_us section
        List<String> currLangIndicator = Collections.singletonList("en_us");
        int langIndicatorIndex = 0;

        List<String> output = new ArrayList<>();
        for (String line: lines) {
            if (line.startsWith("[lang:") && line.endsWith("]")) {
                // Fetch from pre-processed list
                currLangIndicator = availableLangs.get(langIndicatorIndex++);
            } else {
                if (currLangIndicator.contains(targetLang)) {
                    output.add(line);
                }
            }
        }

        return output;
    }

    public static String getFirstTip() {
        return tips.get(0);
    }

    /** Checks to see if any valid tips have been loaded ({@link #getFirstTip()} will return the default tip if this
     * returns false). */
    public static boolean hasAnyTips() {
        return anyTips;
    }

    public static int getTipCount() {
        return tips.size();
    }

    /** @return The tip at the given index, or null if the index is out of bounds. */
    @Nullable
    public static String getTipAt(int index) {
        if (index < 0 || index >= tips.size()) {
            return null;
        }
        return tips.get(index);
    }

    /** @return The tip at the given index. Wraps around if the index was outside of bounds */
    public static String getTip(int index) {
        int count = tips.size();
        if (index < 0) {
            index = (index % count) + count;
        }
        if (index >= count) {
            // Wrap around as that's more useful than
            index = index % count;
        }
        return tips.get(index);
    }

    public static String getTip(long index) {
        return getTip((int) index);
    }
}
