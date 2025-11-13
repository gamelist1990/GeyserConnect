/*
 * @author Koukunn
 */

package org.geysermc.extension.connect.language;

import org.geysermc.extension.connect.GeyserConnect;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LanguageManager {
    private static final String LANG_FOLDER = "lang";
    private static final String DEFAULT_LANG = "en_US";
    
    private final Path langFolder;
    private final Map<String, Map<String, String>> loadedLanguages = new HashMap<>();
    private Map<String, String> currentLanguage;
    
    public LanguageManager() {
        this.langFolder = Paths.get(GeyserConnect.instance().dataFolder().toAbsolutePath().toString(), LANG_FOLDER);
        initializeLangFolder();
    }
    
    private void initializeLangFolder() {
        try {
            if (!Files.exists(langFolder)) {
                Files.createDirectories(langFolder);
                GeyserConnect.instance().logger().info("Created lang folder");
            }
            
            // Create default en_US.lang if it doesn't exist
            Path defaultLangFile = langFolder.resolve(DEFAULT_LANG + ".lang");
            if (!Files.exists(defaultLangFile)) {
                createDefaultLanguageFile(defaultLangFile);
                GeyserConnect.instance().logger().info("Created default language file: " + DEFAULT_LANG + ".lang");
            }
            
            // Load default language
            loadLanguage(DEFAULT_LANG);
            this.currentLanguage = loadedLanguages.get(DEFAULT_LANG);
            
        } catch (IOException e) {
            GeyserConnect.instance().logger().severe("Failed to initialize language folder", e);
        }
    }
    
    private void createDefaultLanguageFile(Path filePath) throws IOException {
        Properties props = new Properties();
        
        // UI Messages
        props.setProperty("ui.title.main_menu", "Main Menu");
        props.setProperty("ui.button.official_servers", "Official Servers");
        props.setProperty("ui.button.geyser_servers", "Geyser Servers");
        props.setProperty("ui.button.custom_servers", "Custom Servers");
        props.setProperty("ui.button.direct_connect", "Direct connect");
        props.setProperty("ui.button.disconnect", "Disconnect");
        props.setProperty("ui.button.back", "Back");
        props.setProperty("ui.button.yes", "Yes");
        props.setProperty("ui.button.no", "No");
        
        // Server menus
        props.setProperty("ui.title.servers", "%s Servers");
        props.setProperty("ui.title.edit_servers", "Edit Servers");
        props.setProperty("ui.content.edit_servers", "Select a server to edit");
        props.setProperty("ui.button.edit_server", "Edit server");
        props.setProperty("ui.button.delete_server", "Delete server");
        props.setProperty("ui.button.add_server", "Add server");
        props.setProperty("ui.button.edit_servers", "Edit servers");
        
        // Add/Edit server forms
        props.setProperty("ui.title.add_server", "Add Server");
        props.setProperty("ui.title.edit_server", "Edit Server");
        props.setProperty("ui.title.direct_connect", "Direct Connect");
        props.setProperty("ui.label.ip", "IP");
        props.setProperty("ui.label.port", "Port");
        props.setProperty("ui.label.online_mode", "Online mode");
        props.setProperty("ui.label.bedrock_server", "Bedrock/Geyser server");
        
        // Server options
        props.setProperty("ui.title.server_options", "Server Options");
        
        // Delete confirmation
        props.setProperty("ui.title.delete_server", "Delete Server");
        props.setProperty("ui.content.delete_server", "Are you sure you want to delete %s?");
        
        // Notice
        props.setProperty("ui.title.notice", "Notice");
        
        try (OutputStream out = Files.newOutputStream(filePath)) {
            props.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "GeyserConnect Language File - English (US)");
        }
    }
    
    public void loadLanguageFromConfig(String languageName) {
        if (languageName == null || languageName.trim().isEmpty()) {
            // Use default language
            this.currentLanguage = loadedLanguages.get(DEFAULT_LANG);
            return;
        }
        
        // Try to load the specified language
        if (!loadedLanguages.containsKey(languageName)) {
            loadLanguage(languageName);
        }
        
        Map<String, String> language = loadedLanguages.get(languageName);
        if (language != null) {
            this.currentLanguage = language;
            GeyserConnect.instance().logger().info("Loaded language: " + languageName);
        } else {
            // Fallback to default if language not found
            GeyserConnect.instance().logger().warning("Language not found: " + languageName + ", using default: " + DEFAULT_LANG);
            this.currentLanguage = loadedLanguages.get(DEFAULT_LANG);
        }
    }
    
    private void loadLanguage(String languageName) {
        try {
            Path langFile = langFolder.resolve(languageName + ".lang");
            
            if (!Files.exists(langFile)) {
                GeyserConnect.instance().logger().warning("Language file not found: " + langFile);
                return;
            }
            
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(langFile);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
            
            Map<String, String> languageMap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                languageMap.put(key, props.getProperty(key));
            }
            
            loadedLanguages.put(languageName, languageMap);
            
        } catch (IOException e) {
            GeyserConnect.instance().logger().severe("Failed to load language: " + languageName, e);
        }
    }
    
    public String getMessage(String key, Object... args) {
        String message = currentLanguage.getOrDefault(key, key);
        
        // If not found in current language, try default
        if (message.equals(key) && currentLanguage != loadedLanguages.get(DEFAULT_LANG)) {
            message = loadedLanguages.get(DEFAULT_LANG).getOrDefault(key, key);
        }
        
        // Format the message with arguments if provided
        if (args.length > 0) {
            try {
                message = String.format(message, args);
            } catch (Exception e) {
                GeyserConnect.instance().logger().warning("Failed to format message: " + key + ", " + e.getMessage());
            }
        }
        
        return message;
    }
    
    public String get(String key) {
        return getMessage(key);
    }
}
