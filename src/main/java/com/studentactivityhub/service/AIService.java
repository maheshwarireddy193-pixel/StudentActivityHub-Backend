package com.studentactivityhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studentactivityhub.dto.AIResumeRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Service
public class AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // Generate a professional resume using Gemini AI
    public String generateResume(AIResumeRequest request) {

        String prompt = """
                Create a professional resume using the following information.

                PERSONAL DETAILS

                Name: %s
                Email: %s
                Phone: %s
                LinkedIn: %s
                GitHub: %s

                EDUCATION

                Education: %s
                Graduation Year: %s
                CGPA: %s

                TECHNICAL SKILLS

                %s

                SOFT SKILLS

                %s

                EXPERIENCE

                %s

                PROJECTS

                %s

                ACHIEVEMENTS PROVIDED BY STUDENT

                %s

                LANGUAGES

                %s

                CAREER OBJECTIVE

                %s

                POSITIONS OF RESPONSIBILITY

                %s

                INSTRUCTIONS

                1. Create a professional, well-organized resume.
                2. Use separate headings for each section.
                3. Present skills as separate items or bullet points.
                4. Organize projects with descriptions and technologies.
                5. Preserve GitHub and LinkedIn links exactly as provided.
                6. Include only information supplied in the input.
                7. Do not invent qualifications, skills, or experience.
                8. Omit optional sections when no information is provided.
                9. Format the result for a downloadable resume.

                """.formatted(
                valueOrDefault(request.getFullName()),
                valueOrDefault(request.getEmail()),
                valueOrDefault(request.getPhone()),
                valueOrDefault(request.getLinkedin()),
                valueOrDefault(request.getGithub()),
                valueOrDefault(request.getEducation()),
                valueOrDefault(request.getGraduationYear()),
                valueOrDefault(request.getCgpa()),
                valueOrDefault(request.getSkills()),
                valueOrDefault(request.getSoftSkills()),
                valueOrDefault(request.getExperience()),
                valueOrDefault(request.getProjects()),
                valueOrDefault(request.getAchievements()),
                valueOrDefault(request.getLanguages()),
                valueOrDefault(request.getCareerObjective()),
                valueOrDefault(request.getPositionsOfResponsibility())
        );

        return callGemini(prompt);
    }

    // Generate title and description from certificate text
    public JsonNode generateCertificateAnalysis(String certificateText) {

        if (certificateText == null || certificateText.isBlank()) {
            throw new IllegalArgumentException(
                    "No readable text was found in the certificate.");
        }

        String prompt = """
                Analyze the certificate text provided below.

                Return ONLY a valid JSON object with exactly these fields:

                {
                  "title": "The actual course, certification, event, or activity name",
                  "description": "A professional description in 2 to 4 sentences"
                }

                Rules:

                1. Generate the title from the actual certificate content.
                2. Do not use the student's manually entered activity title.
                3. Prefer the actual course or certification name printed on the certificate.
                4. Use only information supported by the certificate.
                5. Do not invent skills, achievements, dates, or organizations.
                6. If the certificate identifies a course, use that course name as the title.
                7. Do not include Markdown code fences or extra text outside the JSON.

                CERTIFICATE TEXT:

                %s

                """.formatted(certificateText);

        String response = callGemini(prompt);

        try {
            String cleanedResponse = response.trim();

            // Remove Markdown code fences if Gemini includes them
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse
                        .replaceFirst("^```(?:json)?\\s*", "")
                        .replaceFirst("\\s*```$", "")
                        .trim();
            }

            JsonNode result = objectMapper.readTree(cleanedResponse);

            String title = result.path("title").asText("").trim();
            String description =
                    result.path("description").asText("").trim();

            if (title.isBlank() || description.isBlank()) {
                throw new RuntimeException(
                        "Gemini did not generate both title and description.");
            }

            return result;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not read Gemini's certificate analysis response.", e);
        }
    }

    // Send a prompt to Gemini
    private String callGemini(String prompt) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "Gemini API key is missing. Check GEMINI_API_KEY.");
        }

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new RuntimeException(
                    "Gemini API URL is missing. Check gemini.api.url.");
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            ArrayNode contents = requestBody.putArray("contents");
            ObjectNode content = contents.addObject();

            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            String jsonBody =
                    objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "Gemini API request failed with HTTP status "
                        + statusCode + ". Response: "
                        + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());

            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode()
                    || textNode.isNull()
                    || textNode.asText().isBlank()) {

                throw new RuntimeException(
                        "Gemini returned an empty response. "
                        + "Check the API response.");
            }

            return textNode.asText().trim();

        } catch (HttpTimeoutException e) {

            throw new RuntimeException(
                    "Gemini API request timed out after 180 seconds. "
                    + "Check your internet connection, API availability, "
                    + "and configured model.", e);

        } catch (ConnectException e) {

            throw new RuntimeException(
                    "Could not connect to Gemini. Check your internet "
                    + "connection and API endpoint.", e);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not communicate with Gemini: "
                    + e.getMessage(), e);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Gemini request was interrupted.", e);
        }
    }

    // Handle missing resume fields
    private String valueOrDefault(String value) {

        return value == null || value.isBlank()
                ? "Not provided"
                : value;
    }
}