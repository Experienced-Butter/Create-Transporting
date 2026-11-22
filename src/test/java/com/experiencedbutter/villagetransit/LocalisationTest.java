package com.experiencedbutter.villagetransit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static check: every literal translation key used in code must exist in both language files, and
 * both files must define the same keys. Keys built at runtime (purpose names) are skipped.
 */
class LocalisationTest {

    private static final Pattern TRANSLATABLE = Pattern.compile("translatable\\(\"([a-z0-9_.]+)\"");
    private static final Path ROOT = Path.of("src", "main");
    private static final Path LANG = ROOT.resolve(Path.of("resources", "assets", "create_village_transit", "lang"));

    @Test
    void everyUsedKeyExistsInBothLanguages() throws IOException {
        Set<String> used = collectUsedKeys();
        Set<String> en = readKeys(LANG.resolve("en_us.json"));
        Set<String> zh = readKeys(LANG.resolve("zh_cn.json"));

        Set<String> missingEn = new LinkedHashSet<>(used);
        missingEn.removeAll(en);
        Set<String> missingZh = new LinkedHashSet<>(used);
        missingZh.removeAll(zh);

        assertTrue(missingEn.isEmpty(), "keys missing from en_us.json: " + missingEn);
        assertTrue(missingZh.isEmpty(), "keys missing from zh_cn.json: " + missingZh);
    }

    @Test
    void bothLanguagesDefineTheSameKeys() throws IOException {
        Set<String> en = readKeys(LANG.resolve("en_us.json"));
        Set<String> zh = readKeys(LANG.resolve("zh_cn.json"));

        Set<String> onlyEn = new LinkedHashSet<>(en);
        onlyEn.removeAll(zh);
        Set<String> onlyZh = new LinkedHashSet<>(zh);
        onlyZh.removeAll(en);

        assertTrue(onlyEn.isEmpty(), "only in en_us.json: " + onlyEn);
        assertTrue(onlyZh.isEmpty(), "only in zh_cn.json: " + onlyZh);
    }

    private static Set<String> collectUsedKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        try (Stream<Path> files = Files.walk(ROOT.resolve("java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = TRANSLATABLE.matcher(source);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    // Prefixes such as "state.create_village_transit." are concatenated at runtime.
                    if (!key.endsWith(".")) {
                        keys.add(key);
                    }
                }
            }
        }
        return keys;
    }

    private static Set<String> readKeys(Path path) throws IOException {
        Set<String> keys = new HashSet<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            object.keySet().forEach(keys::add);
        }
        return keys;
    }
}
