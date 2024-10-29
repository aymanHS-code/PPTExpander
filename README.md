# PPTExpander

PPTExpander is meant to enhance PowerPoint presentations by expanding slide content using AI and generating audio narration.

## Table of Contents
- [Features](#features)
- [System Requirements](#system-requirements)
  - [Java Runtime](#java-runtime)
  - [Required API Keys](#required-api-keys)
  - [Possible Issue Fixes](#possible-issue-fixes)
- [Download](#download)
- [First Run](#first-run)
- [Usage](#usage)
- [Configuration](#configuration)
- [Building from Source](#building-from-source)

## Features

- **PowerPoint Parsing**: Extracts text, tables, and images from PowerPoint files
- **AI Content Expansion**: Uses LLMs to expand and clarify slide content
- **Audio Generation**: Creates professional narration using ElevenLabs' text-to-speech API
- **Image Processing**: Handles images through Cloudinary for AI analysis
- **Customizable System Prompt**: Edit the AI instruction prompt to suit your needs
- **Multiple AI Models**:
  - GPT-4o-mini
  - GPT-3.5-turbo-0125
  - Claude-3.5 (Coming Soon!)
- **Coming Soon**:
  - OpenAI Text-to-Speech integration
  - Additional AI models and features

## System Requirements

### Java Runtime
- Java 11 or higher (64-bit) is required
- Download Java from:
  - [Oracle Java](https://www.oracle.com/java/technologies/downloads/#java11) (Commercial)
  - [OpenJDK](https://adoptium.net/) (Free)
- Set JAVA_HOME environment variable to your Java installation directory

### Required API Keys

The application requires the following API keys to function:

1. **OpenAI API Key**
   - Sign up at [OpenAI Platform](https://platform.openai.com/)
   - Create an API key in your account settings
   - Required for: Content expansion and image analysis
   - Pricing: Pay-as-you-go based on token usage

2. **ElevenLabs API Key**
   - Sign up at [ElevenLabs](https://elevenlabs.io/)
   - Create an API key in your account settings
   - Also required: Voice ID (select a voice from your ElevenLabs account)
   - Required for: Audio narration generation
   - Free tier available with limited usage

3. **Cloudinary URL**
   - Sign up at [Cloudinary](https://cloudinary.com/)
   - Find your Cloudinary URL in your account dashboard
   - Format: `cloudinary://API_KEY:API_SECRET@CLOUD_NAME`
   - Required for: Image handling and storage
   - Free tier available

All API keys can be configured in the Settings dialog under the "API Keys" tab.

### Possible Issue Fixes
If you get a JRE-related error:
1. Make sure Java 11+ is installed
2. Set JAVA_HOME environment variable:
   - Right-click 'This PC' > Properties > Advanced System Settings
   - Click 'Environment Variables'
   - Under System Variables, click 'New'
   - Variable name: JAVA_HOME
   - Variable value: Path to your Java installation (e.g., C:\Program Files\Java\jdk-11)
3. Add Java to PATH:
   - In System Variables, find 'Path'
   - Click 'Edit' > 'New'
   - Add '%JAVA_HOME%\bin'

## Download

Download the latest version from the [Releases page](https://github.com/aymanHS-code/PPTExpander/releases).

## First Run

1. Click the "Settings" button to open the settings dialog
2. Enter your API keys in the "API Keys" tab:
   - OpenAI API Key
   - ElevenLabs API Key and Voice ID
   - Cloudinary URL
3. (Optional) Customize the system prompt using the "Edit System Prompt" button

## Usage

1. Click "Select PowerPoint File" to choose your presentation
2. Select the AI model and adjust token limit if needed
3. Click "Expand Presentation" to generate expanded content
4. Review and edit the expanded content for each slide
5. Generate audio narration for individual slides using "Generate audio for this slide"

## Configuration

The application stores its configuration in:
- Windows: `%APPDATA%\PowerPointExpander\config.properties`
- macOS: `~/Library/Application Support/PowerPointExpander\config.properties`
- Linux: `~/.config/PowerPointExpander\config.properties`

## Building from Source

1. Clone the repository:   ```bash
   git clone https://github.com/aymanHS-code/PowerPointExpander.git   ```

2. Build with Maven:   ```bash
   cd PowerPointExpander
   mvn clean package   ```

3. Find the built JAR in `target/PowerPointExpander-1.0.0.jar`
