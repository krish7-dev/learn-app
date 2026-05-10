package com.learnhowyoulearn.service.prompt;

import com.learnhowyoulearn.dto.context.MemoryContext;
import com.learnhowyoulearn.dto.context.TimelineGenerationContext;
import com.learnhowyoulearn.entity.LearningTarget;
import com.learnhowyoulearn.entity.Lecture;
import com.learnhowyoulearn.entity.RevisionItem;
import com.learnhowyoulearn.entity.WeakArea;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PromptBuilderService {

    public static final String PURPOSE_GENERATE_LECTURE_NOTES = "GENERATE_LECTURE_NOTES";
    public static final String PURPOSE_EXPLAIN_AGAIN = "EXPLAIN_AGAIN";
    public static final String PURPOSE_TUTOR_CHAT = "TUTOR_CHAT";
    public static final String PURPOSE_TEACH_BACK_ANALYSIS = "TEACH_BACK_ANALYSIS";
    public static final String PURPOSE_SESSION_SUMMARY = "SESSION_SUMMARY";
    public static final String PURPOSE_TOPIC_NOTE_MERGE = "TOPIC_NOTE_MERGE";
    public static final String PURPOSE_WEAK_AREA_UPDATE = "WEAK_AREA_UPDATE";
    public static final String PURPOSE_GENERATE_TIMELINE = "GENERATE_TIMELINE";

    public String buildBaseSystemPrompt(MemoryContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a personal AI tutor for Krishna, a software engineer learning DSA, LLD, and system design.\n\n");

        if (ctx.getTonePreference() != null) {
            sb.append("Tone: ").append(ctx.getTonePreference()).append("\n\n");
        }

        if (!ctx.getPreferredStyles().isEmpty()) {
            sb.append("Preferred explanation styles: ").append(String.join(", ", ctx.getPreferredStyles())).append("\n");
        }

        if (!ctx.getStruggles().isEmpty()) {
            sb.append("Known struggles (avoid these patterns): ").append(String.join(", ", ctx.getStruggles())).append("\n");
        }

        if (!ctx.getLearningGoals().isEmpty()) {
            sb.append("Learning goals: ").append(String.join("; ", ctx.getLearningGoals())).append("\n");
        }

        if (!ctx.getActiveWeakAreas().isEmpty()) {
            sb.append("\nActive weak areas for this lecture:\n");
            ctx.getActiveWeakAreas().forEach(w -> sb.append("- ").append(w).append("\n"));
        }

        return sb.toString();
    }

    public String buildGenerateNotesPrompt(MemoryContext ctx) {
        String system = buildBaseSystemPrompt(ctx) +
                "\nReturn STRICT valid JSON only — no markdown, no explanation outside the JSON.";

        StringBuilder user = new StringBuilder();
        user.append("Course: ").append(ctx.getCourseTitle()).append("\n");
        user.append("Lecture: ").append(ctx.getLectureTitle()).append("\n\n");

        if (!ctx.getExistingTopics().isEmpty()) {
            user.append("Existing topics in my knowledge base (avoid duplicating, just reference):\n");
            ctx.getExistingTopics().forEach(t -> user.append("- ").append(t).append("\n"));
            user.append("\n");
        }

        if (!ctx.getRecentConfusions().isEmpty()) {
            user.append("Recent confusions from this lecture:\n");
            ctx.getRecentConfusions().forEach(c -> user.append("- ").append(c).append("\n"));
            user.append("\n");
        }

        user.append("Raw lecture transcript:\n---\n").append(ctx.getRawContent()).append("\n---\n\n");

        user.append("""
                Generate RICH, STRUCTURED study notes that teach like a tutor — not a blog summary.

                The "full_clean_notes" field MUST be comprehensive markdown with ALL of these sections:

                ## What is this?
                ## Why do we need it?
                ## Core Idea
                ## Step-by-step Explanation
                ## Formula / Pattern
                ## Dry Run
                (show a concrete example with actual values, walk through every step)
                ## Code Example
                (with inline comments explaining each line)
                ## Edge Cases
                ## Mistakes to Avoid
                ## When to Use
                ## When NOT to Use
                ## Time & Space Complexity
                ## Interview Explanation
                (2-3 sentences: how you'd explain this confidently in an interview)

                Rules for full_clean_notes:
                - Use ## and ### headings
                - Use fenced code blocks with language tags: ```java, ```python, etc.
                - Show dry runs step by step with actual values, not just theory
                - Preserve every example from the transcript — do NOT skip them
                - Use markdown tables where helpful (e.g. complexity comparison, before/after)
                - Bold key terms and formulas
                - DO NOT summarize. Teach in full detail.

                The "simple_explanation" field: conversational markdown for a beginner. Use analogies. 2-3 short paragraphs.
                The "revision_notes" field: tight bullet-point cheat sheet for quick revision before an interview.
                The "practical_usage" field: real-world problems and patterns where this applies.

                Return ONLY this JSON (no markdown wrapper around the JSON itself):
                {
                  "title": "...",
                  "full_clean_notes": "...rich markdown string...",
                  "simple_explanation": "...markdown string...",
                  "practical_usage": "...markdown string...",
                  "examples": [{"title":"...","code":"...","explanation":"...","language":"java|python|javascript|other"}],
                  "mistakes_to_avoid": [
                    {
                      "mistake": "short description of the mistake",
                      "wrong": "wrong code or wrong approach (code block or one-liner)",
                      "right": "correct code or correct approach",
                      "explanation": "why the wrong one fails"
                    }
                  ],
                  "edge_cases": [
                    {
                      "case": "description of the edge case",
                      "example": "concrete example input that triggers it",
                      "how_to_handle": "what to do about it"
                    }
                  ],
                  "revision_notes": "...markdown bullet points...",
                  "interview_questions": ["..."],
                  "flashcards": [{"front":"...","back":"..."}],
                  "practice_questions": ["..."],
                  "weak_area_checks": ["..."],
                  "extracted_topics": [
                    {
                      "name": "...",
                      "importance": "HIGH|MEDIUM|LOW",
                      "coverage_level": "INTRO|INTERMEDIATE|ADVANCED",
                      "evidence": "..."
                    }
                  ]
                }
                """);

        return buildPrompt(system, user.toString());
    }

    public String buildTutorChatPrompt(MemoryContext ctx, String userQuestion) {
        String system = buildBaseSystemPrompt(ctx) +
                "\n\nYou are helping with lecture: " + ctx.getLectureTitle() +
                " from course: " + ctx.getCourseTitle() + ".";

        StringBuilder user = new StringBuilder();

        if (ctx.getLectureNotesText() != null) {
            user.append("Lecture notes (reference only):\n---\n")
                .append(ctx.getLectureNotesText(), 0, Math.min(3000, ctx.getLectureNotesText().length()))
                .append("...\n---\n\n");
        }

        if (!ctx.getRecentChatMessages().isEmpty()) {
            user.append("Recent conversation:\n");
            ctx.getRecentChatMessages().forEach(m -> user.append(m).append("\n"));
            user.append("\n");
        }

        if (ctx.getMemorySummary() != null) {
            user.append("Session summary: ").append(ctx.getMemorySummary()).append("\n\n");
        }

        user.append("Student question: ").append(userQuestion);

        return buildPrompt(system, user.toString());
    }

    public String buildTeachBackPrompt(MemoryContext ctx, String topicName, String studentExplanation) {
        String system = buildBaseSystemPrompt(ctx) +
                "\n\nEvaluate the student's teach-back explanation. Return JSON only.";

        String user = "Topic: " + topicName + "\n\n" +
                "Student's explanation:\n---\n" + studentExplanation + "\n---\n\n" +
                """
                Return ONLY this JSON:
                {
                  "score": 0-100,
                  "understood_correctly": ["..."],
                  "gaps": ["..."],
                  "misconceptions": ["..."],
                  "feedback": "...",
                  "suggested_next": "..."
                }
                """;

        return buildPrompt(system, user);
    }

    public String buildTimelinePrompt(TimelineGenerationContext ctx) {
        LearningTarget target = ctx.getTarget();
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, target.getTargetDate());
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        String system = """
                You are a study planner for Krishna, a software engineer preparing for interviews.
                Rules:
                - Every day MUST have at least 1 MINIMUM-tier item with estimatedMinutes <= 15.
                - All individual items must have estimatedMinutes <= 30. Split larger tasks across days.
                - Never use guilt or shame language in descriptions.
                - Plan realistic days, not perfect days.
                - ADD_TRANSCRIPT and GENERATE_NOTES tasks are prep steps (5 min each). Schedule them before STUDY_LECTURE.
                - NOT_ADDED lectures need: ADD_TRANSCRIPT first, then GENERATE_NOTES on a later day, then STUDY_LECTURE.
                - TRANSCRIPT_ADDED lectures need: GENERATE_NOTES first, then STUDY_LECTURE.
                - NOTES_GENERATED lectures: schedule STUDY_LECTURE + later REVISION/PRACTICE.
                - COMPLETED lectures: only REVISION/PRACTICE if useful.
                - Plan tier MINIMUM = absolute minimum for the day (1 short item). MEDIUM = steady progress. FULL = ideal day.
                - Return STRICT valid JSON only — no markdown, no explanation outside the JSON.
                """;

        StringBuilder user = new StringBuilder();
        user.append("Today: ").append(today.format(fmt)).append("\n");
        user.append("Deadline: ").append(target.getTargetDate().format(fmt)).append("\n");
        user.append("Days remaining: ").append(daysRemaining).append("\n");
        user.append("Priority: ").append(target.getPriority()).append("\n");
        user.append("Daily budget: ").append(target.getDailyMinutes()).append(" min\n\n");

        user.append("Lectures (id | title | module | status | content_status | estimated_minutes):\n");
        for (Lecture l : ctx.getLectures()) {
            user.append(String.format("  %d | %s | %s | %s | %s | %d min%n",
                    l.getId(),
                    l.getTitle(),
                    l.getModuleName() != null ? l.getModuleName() : "-",
                    l.getStatus().name(),
                    l.getContentStatus() != null ? l.getContentStatus() : "NOT_ADDED",
                    l.getEstimatedMinutes() != null ? l.getEstimatedMinutes() : 60));
        }

        if (!ctx.getPendingRevisions().isEmpty()) {
            user.append("\nPending revisions (title | dueAt):\n");
            for (RevisionItem r : ctx.getPendingRevisions()) {
                user.append(String.format("  %s | due %s%n", r.getTitle(), r.getDueAt()));
            }
        }

        if (!ctx.getActiveWeakAreas().isEmpty()) {
            user.append("\nActive weak areas (topic | severity):\n");
            for (WeakArea w : ctx.getActiveWeakAreas()) {
                user.append(String.format("  %s | %s%n", w.getTopic(), w.getSeverity()));
            }
        }

        user.append("""

                Return a JSON object with this exact structure:
                {
                  "days": [
                    {
                      "date": "YYYY-MM-DD",
                      "items": [
                        {
                          "itemType": "ADD_TRANSCRIPT|GENERATE_NOTES|STUDY_LECTURE|REVISION|WEAK_AREA|PRACTICE|TEACH_BACK|BUFFER",
                          "title": "...",
                          "description": "...",
                          "estimatedMinutes": 5-30,
                          "planTier": "FULL|MEDIUM|MINIMUM",
                          "lectureId": null or lecture id number,
                          "topicId": null,
                          "aiReasoning": "one-line reason for scheduling this here"
                        }
                      ]
                    }
                  ]
                }

                Generate a plan covering today through the deadline. Every day must have at least one MINIMUM item.
                """);

        return buildPrompt(system, user.toString());
    }

    private String buildPrompt(String system, String user) {
        return system + "\n\n<<<USER_MESSAGE>>>\n" + user;
    }

    public String[] splitPrompt(String combined) {
        String[] parts = combined.split("\n\n<<<USER_MESSAGE>>>\n", 2);
        if (parts.length == 2) return parts;
        return new String[]{combined, ""};
    }
}
