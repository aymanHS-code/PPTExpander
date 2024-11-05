package com.powerpoint.expander;

import javax.swing.*;

import java.awt.*;
import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {
    private static final Logger LOGGER = Logger.getLogger(Settings.class.getName());
    private static final String APP_DIR_NAME = "PowerPointExpander";
    private static final String SETTINGS_FILE = "config.properties";
    private static final Path APP_DIR;
    private static final Path SETTINGS_PATH;
    private static Properties properties;
    
    public static final String DEFAULT_SYSTEM_PROMPT = 
        "As an AI professor, explain and expand on PowerPoint slide content for students in a manner to clarify the content given. " +
        "Follow these guidelines:\n\n" +
        "1. Present information in a clear, informative manner without directly addressing the listener.\n" +
        "2. Start explanations immediately with the content, avoiding phrases like 'This slide discusses...' or 'The slide titled...'\n" +
        "3. Explain concepts in detail, following the order presented in the original content.\n" +
        "4. Use and elaborate on examples provided to enhance understanding.\n" +
        "5. Present information in flowing paragraphs, using bullet points sparingly for lists or key points.\n" +
        "6. Maintain any structure of bullet points or numbered lists from the original content when necessary.\n" +
        "7. Provide additional context or examples only when it directly supports the content.\n" +
        "8. Break down complex ideas into simpler terms, always referring back to the original content.\n" +
        "9. Ensure the explanation is engaging and easy to follow, tailored for listening rather than reading.\n" +
        "10. Maintain a pace suitable for listening comprehension.\n" +
        "11. Do not introduce new topics or concepts not mentioned or implied in the original content.\n" +
        "12. Avoid phrases like 'in this lesson,' 'you will learn,' or directly addressing the listener as 'you.'\n" +
        "13. Focus on explaining the content objectively, as if providing information rather than teaching a lesson.\n" +
        "14. Do not use unreadable characters like emojis, symbols, or special characters (only use standard punctuation and numbers).\n\n" +
        "The primary focus is to explain and clarify the information presented, not to add extensive new information or frame it as a personal lesson.";

    static {
        // Determine the application directory in the user's home folder
        String userHome = System.getProperty("user.home");
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // On Windows, use AppData/Roaming
            APP_DIR = Paths.get(userHome, "AppData", "Roaming", APP_DIR_NAME);
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // On macOS, use Library/Application Support
            APP_DIR = Paths.get(userHome, "Library", "Application Support", APP_DIR_NAME);
        } else {
            // On Linux/Unix, use .config
            APP_DIR = Paths.get(userHome, ".config", APP_DIR_NAME);
        }
        SETTINGS_PATH = APP_DIR.resolve(SETTINGS_FILE);
        
        // Create the application directory if it doesn't exist
        try {
            if (!APP_DIR.toFile().exists()) {
                APP_DIR.toFile().mkdirs();
                LOGGER.info("Created application directory: " + APP_DIR);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to create application directory: " + e.getMessage());
        }

        properties = new Properties();
        loadSettings();
    }

    public static void loadSettings() {
        if (SETTINGS_PATH.toFile().exists()) {
            try (FileInputStream in = new FileInputStream(SETTINGS_PATH.toFile())) {
                properties.load(in);
                LOGGER.info("Loaded settings from: " + SETTINGS_PATH);
            } catch (IOException e) {
                LOGGER.warning("Failed to load settings, using defaults: " + e.getMessage());
                setDefaults();
            }
        } else {
            LOGGER.info("No settings file found, creating defaults at: " + SETTINGS_PATH);
            setDefaults();
            saveSettings(); // Save default settings to create the file
        }
    }

    private static void setDefaults() {
        properties.setProperty("openai.api.key", "");
        properties.setProperty("elevenlabs.api.key", "");
        properties.setProperty("elevenlabs.voice.id", "");
        properties.setProperty("cloudinary.url", "");
        properties.setProperty("system.prompt", DEFAULT_SYSTEM_PROMPT);
    }

    public static void saveSettings() {
        try (FileOutputStream out = new FileOutputStream(SETTINGS_PATH.toFile())) {
            properties.store(out, "PowerPoint Expander Settings");
            LOGGER.info("Saved settings to: " + SETTINGS_PATH);
        } catch (IOException e) {
            LOGGER.severe("Failed to save settings: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                "Failed to save settings: " + e.getMessage(),
                "Settings Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String get(String key) {
        String value = properties.getProperty(key, "");
        
        // Special handling for cloudinary URL to remove prefix if present
        if (key.equals("cloudinary.url") && value.startsWith("CLOUDINARY_URL=")) {
            return value.substring("CLOUDINARY_URL=".length());
        }
        return value;
    }

    public static void set(String key, String value) {
        // Special handling for cloudinary URL to ensure correct format
        if (key.equals("cloudinary.url")) {
            // Remove prefix if it exists
            if (value.startsWith("CLOUDINARY_URL=")) {
                value = value.substring("CLOUDINARY_URL=".length());
            }
        }
        properties.setProperty(key, value);
    }

    public static void showSettingsDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Settings", true);
        dialog.setLayout(new BorderLayout());
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // API Keys Tab
        JPanel apiPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        addSettingField(apiPanel, gbc, "OpenAI API Key:", "openai.api.key");
        addSettingField(apiPanel, gbc, "ElevenLabs API Key:", "elevenlabs.api.key");
        addSettingField(apiPanel, gbc, "ElevenLabs Voice ID:", "elevenlabs.voice.id");
        addSettingField(apiPanel, gbc, "Cloudinary URL:", "cloudinary.url");
        
        // Add a note about OpenAI TTS
        JLabel ttsNote = new JLabel("<html><i>Note: OpenAI Text-to-Speech integration coming soon!</i></html>");
        ttsNote.setForeground(new Color(100, 100, 100));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        apiPanel.add(ttsNote, gbc);
        
        // Reset gridwidth for other components
        gbc.gridwidth = 1;
        
        tabbedPane.addTab("API Keys", new JScrollPane(apiPanel));
        
        // Other Settings Tab (kept for future settings)
        JPanel otherPanel = new JPanel(new GridBagLayout());
        tabbedPane.addTab("Other", new JScrollPane(otherPanel));
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset to Defaults");

        saveButton.addActionListener(e -> {
            // Save API keys
            Component[] components = apiPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    JTextField field = (JTextField) comp;
                    if (field.getName() != null) {
                        set(field.getName(), field.getText());
                    }
                }
            }
            saveSettings();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        resetButton.addActionListener(e -> {
            setDefaults();
            dialog.dispose();
            showSettingsDialog(parent);
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);

        dialog.add(tabbedPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static void addSettingField(JPanel panel, GridBagConstraints gbc, String label, String key) {
        panel.add(new JLabel(label), gbc);
        
        JTextField field = new JTextField(get(key), 30);
        field.setName(key);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(field, gbc);
        
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
    }
} 