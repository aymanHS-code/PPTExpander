package com.powerpoint.expander;

import org.json.JSONObject;
import org.json.JSONArray;
import javax.swing.*;
import java.awt.*;
import java.io.File;
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
        generateAudioButton = new JButton("Generate Audio");
        generateAudioButton.setEnabled(false);

        topPanel.add(selectFileButton);
        topPanel.add(expandButton);
        topPanel.add(generateAudioButton);

        tabbedPane = new JTabbedPane();

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
        generateAudioButton.addActionListener(e -> generateAudio());

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
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    statusLabel.setText("Parsing PowerPoint...");
                    List<String> slideContents = PowerPointParser.parseSlides(selectedFile);

                    statusLabel.setText("Expanding content with OpenAI...");
                    return OpenAIExpander.expandSlideContents(slideContents);
                }

                @Override
                protected void done() {
                    try {
                        String expandedContent = get();
                        displayExpandedContent(expandedContent);
                    } catch (Exception e) {
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

    private void displayExpandedContent(String expandedContent) {
        JSONObject expandedContentJson = new JSONObject(expandedContent);

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

        generateAudioButton.setEnabled(true);
        statusLabel.setText("Content expanded. You can now edit the content and generate audio.");
    }

    private void generateAudio() {
        try {
            statusLabel.setText("Generating speech with ElevenLabs...");
            String pptName = selectedFile.getName().replaceFirst("[.][^.]+$", "");
            String outputDir = selectedFile.getParent() + File.separator + pptName + "_audio";
            new File(outputDir).mkdirs();

            for (int i = 0; i < slideTextAreas.length; i++) {
                String slideContent = slideTextAreas[i].getText();
                String outputPath = outputDir + File.separator + "slide" + (i + 1) + ".mp3";
                ElevenLabsTTS.generateSpeech(slideContent.trim(), outputPath);
                statusLabel.setText("Generated audio for slide " + (i + 1) + " of " + slideTextAreas.length);
            }

            statusLabel.setText("Audio generation complete! Audio files saved in: " + outputDir);
            JOptionPane.showMessageDialog(frame, "Audio generation complete!\nAudio files saved in: " + outputDir);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error generating audio: " + e.getMessage());
            JOptionPane.showMessageDialog(frame, "Error generating audio: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
