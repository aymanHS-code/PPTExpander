# PowerPoint Expander

PowerPoint Expander is a Java application that enhances PowerPoint presentations by expanding slide content using AI and generating audio narration.

## Features

- **PowerPoint Parsing**: Extracts text, tables, and images from PowerPoint files
- **AI Content Expansion**: Uses OpenAI's GPT models to expand and clarify slide content
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

### Windows Users
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

## Requirements
<<<<<<< Updated upstream
- Java 11 or higher
- OpenAI API key
- Elevenlabs API key

## Setup
1. Clone the repository
2. Create a `.env` file in the root directory of the project
3. Add your API keys to the `.env` file:
   ```
   OPENAI_API_KEY=your_api_key_here
   ELEVENLABS_API_KEY=your_api_key_here
   ELEVENLABS_VOICE_ID=your_voice_id_here
   CLOUDINARY_URL=your_cloudinary_url_here
   ```
4. Build and install requirements using `mvn clean install`
5. Run the built `.jar` file or execute from terminal using `java -jar target/<name-of-jar-file>.jar`

## To Be Done
- Display each slide as a separate tab to edit
- Audio generation
=======

- API Keys for:
  - OpenAI
  - ElevenLabs
  - Cloudinary

## Download

Download the latest version from the [Releases page](https://github.com/yourusername/PPTExpander/releases).

## Installation

1. Ensure you have Java 11 or higher installed
2. Download `PowerPointExpander-1.0.0.jar` from the releases page
3. Double-click the JAR file or run:   ```bash
   java -jar PowerPointExpander-1.0.0.jar   ```

## First Run

1. Click the "Settings" button to open the settings dialog
2. Enter your API keys in the "API Keys" tab
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
- macOS: `~/Library/Application Support/PowerPointExpander/config.properties`
- Linux: `~/.config/PowerPointExpander/config.properties`

## Building from Source

1. Clone the repository:   ```bash
   git clone https://github.com/yourusername/PowerPointExpander.git   ```

2. Build with Maven:   ```bash
   cd PowerPointExpander
   mvn clean package   ```

3. Find the built JAR in `target/PowerPointExpander-1.0.0.jar`
>>>>>>> Stashed changes
