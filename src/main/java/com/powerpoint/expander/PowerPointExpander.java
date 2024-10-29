package com.powerpoint.expander;

import org.json.JSONObject;

import org.json.JSONArray;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PowerPointExpander {
    private JFrame frame;
    private JButton selectFileButton;
    private JButton expandButton;
    private JButton generateAudioButton;
    private JLabel statusLabel;
    private JTextArea[] slideTextAreas;
    private JTabbedPane tabbedPane;
    private JProgressBar progressBar;
    private File selectedFile;
    private JTextField maxTokensField;
    private JComboBox<String> modelSelector;
    private JLabel pricingLabel;
    private JButton settingsButton;
    private static final Logger LOGGER = Logger.getLogger(PowerPointExpander.class.getName());

    private static final Map<String, String> MODEL_PRICING = new HashMap<>();
    static {
        MODEL_PRICING.put("gpt-4o-mini", "Input: $0.000150 / 1K tokens, Output: $0.000600 / 1K tokens");
        MODEL_PRICING.put("gpt-3.5-turbo-0125", "Input: $0.0005 / 1K tokens, Output: $0.0015 / 1K tokens");
        MODEL_PRICING.put("claude-3.5 (soon)", "Coming soon!");
        // Add more models and their pricing here
    }

    public PowerPointExpander() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("PowerPoint Expander");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);
        frame.setLayout(new BorderLayout());

        // Add this code to set the icon
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            frame.setIconImage(icon.getImage());
        } catch (Exception e) {
            LOGGER.warning("Could not load application icon: " + e.getMessage());
        }

        // Create a main top panel to hold both status and action buttons
        JPanel mainTopPanel = new JPanel(new BorderLayout());

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("No file selected", SwingConstants.CENTER);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        mainTopPanel.add(statusPanel, BorderLayout.NORTH);

        // Action buttons panel - now using FlowLayout.CENTER
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        selectFileButton = new JButton("Select PowerPoint File");
        expandButton = new JButton("Expand Presentation");
        expandButton.setEnabled(false);  // Initially disabled
        generateAudioButton = new JButton("Generate audio for this slide");
        generateAudioButton.setEnabled(false);

        actionPanel.add(selectFileButton);
        actionPanel.add(expandButton);
        actionPanel.add(generateAudioButton);
        mainTopPanel.add(actionPanel, BorderLayout.CENTER);

        // Bottom panel for settings and options
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // Progress bar panel - now at the top of bottom panel
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        
        // Options and buttons panel
        JPanel optionsAndButtonsPanel = new JPanel(new BorderLayout());
        
        // Options panel (left side of bottom)
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Model selection
        JPanel modelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        modelPanel.add(new JLabel("Model:"));
        modelSelector = new JComboBox<>(MODEL_PRICING.keySet().toArray(new String[0]));
        modelSelector.addActionListener(e -> updatePricingInfo());
        modelSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ("claude-3.5 (soon)".equals(value)) {
                    c.setEnabled(false);
                }
                return c;
            }
        });
        modelPanel.add(modelSelector);
        pricingLabel = new JLabel();
        modelPanel.add(pricingLabel);
        optionsPanel.add(modelPanel);

        // Token limit
        JPanel tokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        tokenPanel.add(new JLabel("Max Tokens:"));
        maxTokensField = new JTextField("2000", 5);
        tokenPanel.add(maxTokensField);
        JLabel tokenInfoLabel = new JLabel("?");
        tokenInfoLabel.setToolTipText("<html>Tokens are pieces of words used for natural language processing.<br>" +
                "The max tokens parameter sets the maximum length of the generated text.<br>" +
                "Higher values allow for longer responses but may increase processing time and costs.</html>");
        tokenInfoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tokenPanel.add(tokenInfoLabel);
        optionsPanel.add(tokenPanel);

        // Buttons panel (right side of bottom)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        
        // System prompt button
        JButton systemPromptButton = new JButton("Edit System Prompt");
        systemPromptButton.addActionListener(e -> showSystemPromptDialog());
        buttonsPanel.add(systemPromptButton);

        // Settings button
        settingsButton = new JButton("Settings");
        settingsButton.addActionListener(e -> Settings.showSettingsDialog(frame));
        buttonsPanel.add(settingsButton);

        optionsAndButtonsPanel.add(optionsPanel, BorderLayout.WEST);
        optionsAndButtonsPanel.add(buttonsPanel, BorderLayout.EAST);
        
        bottomPanel.add(optionsAndButtonsPanel, BorderLayout.CENTER);

        // Initialize tab pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> updateGenerateAudioButton());

        // Add all panels to frame
        frame.add(mainTopPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(tabbedPane), BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Add action listeners
        selectFileButton.addActionListener(e -> selectFile());
        expandButton.addActionListener(e -> expandPresentation());
        generateAudioButton.addActionListener(e -> generateAudioForCurrentSlide());

        updatePricingInfo();
        frame.setVisible(true);
    }

    private void updatePricingInfo() {
        String selectedModel = (String) modelSelector.getSelectedItem();
        String pricingInfo = MODEL_PRICING.get(selectedModel);
        pricingLabel.setText(pricingInfo);
        pricingLabel.setToolTipText(pricingInfo);
        frame.revalidate();  // Revalidate the frame to update the layout
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            statusLabel.setText("Selected file: " + selectedFile.getName());
            expandButton.setEnabled(true);
        }
    }

    private void expandPresentation() {
        if (selectedFile != null) {
            SwingWorker<JSONObject, Void> worker = new SwingWorker<JSONObject, Void>() {
                @Override
                protected JSONObject doInBackground() throws Exception {
                    statusLabel.setText("Parsing PowerPoint...");
                    List<SlideContent> slideContents = PowerPointParser.parseSlides(selectedFile);

                    statusLabel.setText("Expanding content with OpenAI...");
                    int maxTokens = Integer.parseInt(maxTokensField.getText());
                    String selectedModel = (String) modelSelector.getSelectedItem();
                    String jsonResponse = OpenAIExpander.expandSlideContents(slideContents, maxTokens, selectedModel);
                    LOGGER.info("JSON response from OpenAIExpander: " + jsonResponse);
                    return new JSONObject(jsonResponse);
                }

                @Override
                protected void done() {
                    try {
                        JSONObject expandedContent = get();
                        LOGGER.info("Expanded content received in PowerPointExpander: " + expandedContent.toString(2));
                        displayExpandedContent(expandedContent);
                    } catch (Exception e) {
                        LOGGER.severe("Error in PowerPointExpander: " + e.getMessage());
                        e.printStackTrace();
                        statusLabel.setText("Error: " + e.getMessage());
                    } finally {
                        progressBar.setVisible(false);
                        expandButton.setEnabled(true);
                    }
                }
            };

            expandButton.setEnabled(false);
            progressBar.setVisible(true);
            worker.execute();
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a PowerPoint file first.");
        }
    }

    private void displayExpandedContent(JSONObject expandedContentJson) {
        if (expandedContentJson.has("error")) {
            statusLabel.setText("Error: " + expandedContentJson.getString("error"));
            return;
        }

        JSONArray slidesArray = expandedContentJson.getJSONArray("slides");
        int slideCount = slidesArray.length();
        slideTextAreas = new JTextArea[slideCount];

        tabbedPane.removeAll();

        for (int i = 0; i < slideCount; i++) {
            JSONObject slideObject = slidesArray.getJSONObject(i);
            String expandedContentParsed = slideObject.getString("expandedContent");
            
            JTextArea slideTextArea = new JTextArea(expandedContentParsed);
            slideTextArea.setWrapStyleWord(true);
            slideTextArea.setLineWrap(true);
            JScrollPane scrollPane = new JScrollPane(slideTextArea);
            tabbedPane.addTab("Slide " + (i + 1), scrollPane);
            slideTextAreas[i] = slideTextArea;
        }

        updateGenerateAudioButton();
        statusLabel.setText("Content expanded. You can now edit the content and generate audio for each slide.");
    }

    private void generateAudioForCurrentSlide() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(frame, "Please select a slide first.", "No Slide Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusLabel.setText("Generating speech with ElevenLabs...");
            String pptName = selectedFile.getName().replaceFirst("[.][^.]+$", "");
            String outputDir = selectedFile.getParent() + File.separator + pptName + "_audio";
            new File(outputDir).mkdirs();

            String slideContent = slideTextAreas[selectedIndex].getText();
            String outputPath = outputDir + File.separator + "slide" + (selectedIndex + 1) + ".mp3";

            try {
                ElevenLabsTTS.generateSpeech(slideContent.trim(), outputPath);
                statusLabel.setText("Generated audio for slide " + (selectedIndex + 1));
                JOptionPane.showMessageDialog(frame, "Audio generated successfully for slide " + (selectedIndex + 1) + ".\nSaved to: " + outputPath);
            } catch (IOException e) {
                LOGGER.severe("Error generating audio for slide " + (selectedIndex + 1) + ": " + e.getMessage());
                statusLabel.setText("Error generating audio for slide " + (selectedIndex + 1) + ": " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Error generating audio for slide " + (selectedIndex + 1) + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            LOGGER.severe("Error in audio generation process: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Error in audio generation process: " + e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error in audio generation process: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateGenerateAudioButton() {
        generateAudioButton.setEnabled(tabbedPane.getTabCount() > 0);
    }

    private void showSystemPromptDialog() {
        JDialog dialog = new JDialog(frame, "Edit System Prompt", true);
        dialog.setLayout(new BorderLayout());

        JTextArea promptArea = new JTextArea(Settings.get("system.prompt"), 20, 60);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(promptArea);

        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        JButton resetButton = new JButton("Reset to Default");

        saveButton.addActionListener(e -> {
            Settings.set("system.prompt", promptArea.getText());
            Settings.saveSettings();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        resetButton.addActionListener(e -> {
            promptArea.setText(Settings.DEFAULT_SYSTEM_PROMPT);
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(resetButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        // Configure logging
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        Logger.getLogger("").setLevel(Level.INFO);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.INFO);

        SwingUtilities.invokeLater(() -> {
            new PowerPointExpander();
        });
    }
}
