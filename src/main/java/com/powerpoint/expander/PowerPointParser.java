package com.powerpoint.expander;

import org.apache.poi.xslf.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class PowerPointParser {
    private static final Logger LOGGER = Logger.getLogger(PowerPointParser.class.getName());
    private static final Cloudinary cloudinary;

    static {
        String cloudinaryUrl = Settings.get("cloudinary.url");
        if (cloudinaryUrl.isEmpty()) {
            LOGGER.warning("Cloudinary URL is not configured in settings");
            cloudinary = null;
        } else {
            cloudinary = new Cloudinary(cloudinaryUrl);
        }
    }

    public static List<SlideContent> parseSlides(File file) throws IOException {
        List<SlideContent> slideContents = new ArrayList<>();
        LOGGER.info("Starting to parse PowerPoint file: " + file.getName());
        
        // Validate file extension
        if (!file.getName().toLowerCase().endsWith(".pptx")) {
            throw new IOException("Invalid file format. Only .pptx files are supported.");
        }

        // Validate file exists and is readable
        if (!file.exists() || !file.canRead()) {
            throw new IOException("File does not exist or cannot be read: " + file.getPath());
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            // Validate file size
            if (file.length() == 0) {
                throw new IOException("File is empty: " + file.getPath());
            }

            try (XMLSlideShow ppt = new XMLSlideShow(fis)) {
                int slideCount = ppt.getSlides().size();
                LOGGER.info("PowerPoint file contains " + slideCount + " slides");
                
                for (int i = 0; i < slideCount; i++) {
                    XSLFSlide slide = ppt.getSlides().get(i);
                    LOGGER.info("Parsing slide " + (i + 1));
                    SlideContent content = new SlideContent();
                    StringBuilder textContent = new StringBuilder();
                    
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            LOGGER.info("Parsing text shape in slide " + (i + 1));
                            XSLFTextShape textShape = (XSLFTextShape) shape;
                            textContent.append(textShape.getText()).append("\n");
                        } else if (shape instanceof XSLFTable) {
                            LOGGER.info("Parsing table in slide " + (i + 1));
                            XSLFTable table = (XSLFTable) shape;
                            content.setTable(parseTable(table));
                        } else if (shape instanceof XSLFPictureShape) {
                            LOGGER.info("Parsing image in slide " + (i + 1));
                            XSLFPictureShape picture = (XSLFPictureShape) shape;
                            content.setImageUrl(uploadAndGetImageUrl(picture));
                        }
                    }
                    
                    content.setText(textContent.toString().trim());
                    slideContents.add(content);
                    LOGGER.info("Finished parsing slide " + (i + 1));
                }
            } catch (org.apache.poi.ooxml.POIXMLException e) {
                LOGGER.severe("Error parsing PowerPoint file: " + e.getMessage());
                Throwable cause = e.getCause();
                if (cause instanceof org.apache.poi.openxml4j.exceptions.InvalidFormatException) {
                    throw new IOException("The PowerPoint file appears to be corrupted or is not a valid .pptx file. Please ensure you're using a valid PowerPoint file.", e);
                } else {
                    throw new IOException("Error reading PowerPoint file. The file might be corrupted or in an unsupported format.", e);
                }
            }
        }
        
        LOGGER.info("Finished parsing PowerPoint file: " + file.getName());
        return slideContents;
    }

    private static String parseTable(XSLFTable table) {
        StringBuilder tableContent = new StringBuilder();
        int rowCount = table.getNumberOfRows();
        LOGGER.info("Parsing table with " + rowCount + " rows");
        
        for (int i = 0; i < rowCount; i++) {
            XSLFTableRow row = table.getRows().get(i);
            int cellCount = row.getCells().size();
            for (int j = 0; j < cellCount; j++) {
                XSLFTableCell cell = row.getCells().get(j);
                tableContent.append(cell.getText());
                if (j < cellCount - 1) {
                    tableContent.append(" | ");
                }
            }
            tableContent.append("\n");
        }
        LOGGER.info("Finished parsing table");
        return tableContent.toString().trim();
    }

    private static String uploadAndGetImageUrl(XSLFPictureShape picture) throws IOException {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary URL is not configured. Please set it in Settings.");
        }

        byte[] pictureData = picture.getPictureData().getData();
        LOGGER.info("Uploading image to Cloudinary, size: " + pictureData.length + " bytes");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(pictureData, ObjectUtils.emptyMap());
        String imageUrl = (String) uploadResult.get("secure_url");
        LOGGER.info("Uploaded image to Cloudinary: " + imageUrl);
        return imageUrl;
    }
}
