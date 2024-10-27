package com.powerpoint.expander;

import org.json.JSONObject;
import org.json.JSONArray;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
    private List<String> expandedContents;
    private static final Logger LOGGER = Logger.getLogger(PowerPointExpander.class.getName());

    public PowerPointExpander() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("PowerPoint Expander");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        selectFileButton = new JButton("Select PowerPoint File");
        expandButton = new JButton("Expand Presentation");
        generateAudioButton = new JButton("Generate audio for this slide");
        generateAudioButton.setEnabled(false);

        topPanel.add(selectFileButton);
        topPanel.add(expandButton);
        topPanel.add(generateAudioButton);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> updateGenerateAudioButton());

        statusLabel = new JLabel("No file selected", SwingConstants.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        selectFileButton.addActionListener(e -> selectFile());
        expandButton.addActionListener(e -> expandPresentation());
        generateAudioButton.addActionListener(e -> generateAudioForCurrentSlide());

        frame.setVisible(true);
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
                    List<String> slideContents = PowerPointParser.parseSlides(selectedFile);

                    statusLabel.setText("Expanding content with OpenAI...");
                    String jsonResponse = OpenAIExpander.expandSlideContents(slideContents);
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

    public static void main(String[] args) {
        // Configure logging
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        Logger.getLogger("").setLevel(Level.INFO);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.INFO);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new PowerPointExpander();
            }
        });
    }
}
