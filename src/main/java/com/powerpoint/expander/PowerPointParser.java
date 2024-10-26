package com.powerpoint.expander;

import org.apache.poi.xslf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PowerPointParser {
    private static final Logger LOGGER = Logger.getLogger(PowerPointParser.class.getName());

    public static List<String> parseSlides(File file) throws IOException {
        List<String> slideContents = new ArrayList<>();
        LOGGER.info("Starting to parse PowerPoint file: " + file.getName());

        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            LOGGER.info("PowerPoint file opened successfully. Total slides: " + ppt.getSlides().size());

            for (int i = 0; i < ppt.getSlides().size(); i++) {
                XSLFSlide slide = ppt.getSlides().get(i);
                LOGGER.info("Parsing slide " + (i + 1));

                StringBuilder slideContent = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        LOGGER.fine("Found text shape: " + text);
                        slideContent.append(text).append("\n");
                    } else {
                        LOGGER.fine("Found non-text shape: " + shape.getShapeName());
                    }
                }
                String content = slideContent.toString().trim();
                LOGGER.info("Slide " + (i + 1) + " content: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content));
                slideContents.add(content);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing PowerPoint file", e);
            throw new IOException("Error parsing PowerPoint file", e);
        }

        LOGGER.info("Finished parsing PowerPoint file. Total slides parsed: " + slideContents.size() + "\n" + slideContents.toString());
        return slideContents;
    }
}
