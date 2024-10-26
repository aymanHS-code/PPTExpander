# PPTExpander
### A tool to expand PowerPoint presentations using AI-generated content

Taking a simple presentation as input and generates more detailed content for each slide, helping users create more comprehensive and informative presentations quickly.

## Requirements
- Java 11 or higher
- OpenAI API key
- Elevenlabs API key

## Setup
1. Clone the repository
2. Create a `.env` file in the root directory of the project
3. Add your OpenAI & Elevenlabs API key to the `.env` file:
   ```
   OPENAI_API_KEY=your_api_key_here
   ELEVENLABS_API_KEY=your_api_key_here
   ```
4. Build and install requirements using `mvn clean install`
5. Run the built `.jar` file or execute from terminal using `java -jar target/<name-of-jar-file>.jar`

## To Be Done
- Display each slide as a separate tab to edit
- Audio generation