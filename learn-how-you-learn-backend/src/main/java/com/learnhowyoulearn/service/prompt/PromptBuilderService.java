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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        user.append("Cleaned lecture transcript:\n---\n").append(ctx.getRawContent()).append("\n---\n\n");

        user.append("""
                For "suggested_module": return the most suitable 1-3 word grouping label for this lecture within its course. Examples for a DSA course: "Arrays", "Linked Lists", "Trees", "Graphs", "Dynamic Programming", "Searching", "Sorting", "Recursion", "Sliding Window", "Two Pointers". Match the topic area of the lecture content, not a generic label.

                CORE RULES — READ BEFORE GENERATING:
                - You MUST preserve every distinct teachable concept from the transcript, including from the doubt/Q&A session.
                - Do NOT collapse or genericise lecture-specific examples. If the instructor used a cricket scoreboard, retain that exact example verbatim.
                - Do NOT skip side concepts just because they appear minor or appear in a doubt session. If it is educational, include it.
                - Only skip: pure admin chatter, greetings, attendance calls, feedback requests, and unrelated logistics.
                - Ignore any remaining course roadmap, homework, assignment, doubt-solving, contact, recording, career, or closing content. Generate notes only from technical learning content.
                - Do NOT summarize. Teach in full detail.
                - Correct minor mathematical wording issues if the cleaned transcript clearly implies the correct concept. For example, for repeated integer division by 2 until reaching 1, the answer is floor(log₂(n)), not the greatest power of 2 itself.

                STRICT SOURCE-GROUNDING RULE:
                - Generate notes only from the provided cleaned transcript.
                - Do not add concepts, mistakes, interview questions, examples, formulas, warnings, or practical usages that are not clearly present in the transcript.
                - Do not reuse examples or concepts from previous lectures or prompt examples.
                - If a section has no relevant content from the transcript, return an empty list or omit that item instead of inventing content.
                - Before finalizing, check every generated mistake, flashcard, interview question, and practice question. Remove anything not directly supported by the transcript.
                - Do not include course prerequisites, class logistics, homework/assignment logistics, upcoming module roadmap, doubt-solving logistics, or instructor contact details in the final notes.
                - Generate practical_usage only if it is directly supported by the transcript or is a very standard and obvious application of the concept taught. Do not add loosely related concepts.
                - Correct minor mathematical wording issues when the transcript clearly implies the correct concept.
                - When giving examples with concrete numbers, explicitly state the assumed value of n. Do not silently replace symbolic n with a specific number without declaring it.
                - When using a concrete value for n in examples, make sure the arithmetic exactly matches the loop condition. For loops like j = 1; j <= n; j = j * 2, the values include 1, so the iteration count is floor(log₂(n)) + 1.
                - When giving a concrete dry run or example, always state the assumed input value before listing the steps. For example: "For n = 5" or "For n = 7". Do not start listing steps without declaring the input first.
                - For conceptual lectures, preserve the reasoning chain behind why a concept is needed, not just the final definition. Include the "why" behind comparisons, tradeoffs, and limitations.
                - When generating truth tables, use separate clean markdown tables for AND, OR, and XOR. Do not merge them. Each table must follow this exact format — one row per line, with a separator row after the header:
                  | a | b | result |
                  |---|---|--------|
                  | 0 | 0 | 0      |
                  | 0 | 1 | 0      |
                  | 1 | 0 | 0      |
                  | 1 | 1 | 1      |
                  Do not collapse any table into a single inline pipe-separated string. Do not mix tab-separated and pipe-separated formats across tables.
                - For instructor examples and analogies, preserve exact numeric values and calculations when available.
                - When the transcript gives a full dry-run array/table, include the completed table or final computed array. Do not use "..." for important computed examples.
                - Avoid adding generic applications that were not taught unless clearly marked as an extension.

                IMPORTANT CORRECTION RULE — APPLY EVERYWHERE IN THE OUTPUT:
                If the transcript says prefix sum works because of "associativity", do NOT repeat that wording as-is. Apply this correction throughout full_clean_notes, mistakes_to_avoid, flashcards, revision_notes, and edge_cases:
                - The key requirement for prefix subtraction is that the earlier prefix contribution can be removed/cancelled. Sum/count work because we can subtract; XOR works because XOR cancels itself.
                - Max and min ARE associative, but prefixMax[R] minus prefixMax[L-1] is NOT valid — you cannot subtract/cancel a previous max contribution to get range max.
                - NEVER write "max/min are not associative." Instead write: "max/min do not support prefix subtraction/cancellation — you cannot remove a previous prefix max/min contribution."
                - For edge cases about empty/single-element ranges: a single-element range (L == R) is always valid. An empty range (L > R) is not expected unless the problem explicitly allows invalid queries — do not treat it as a common case.

                The "full_clean_notes" field MUST be comprehensive markdown with EXACTLY these 7 sections in order:

                ## 1. Main Lecture Concepts
                (All primary topics taught in the lecture. For each concept: what it is, why it matters, the core idea, step-by-step explanation, formula/pattern if any, dry run with actual values, code example with inline comments, time & space complexity.)

                ## 2. Supporting Concepts / Recap
                (Any prerequisite or background concepts the instructor recapped or referenced. Explain each one fully — do not just mention them.)

                ## 3. Problems Solved
                (Every problem or exercise the instructor walked through. For each: problem statement, approach, dry run, code with comments, complexity.)

                ## 4. Instructor Examples and Analogies
                (Every real-world example, analogy, or story the instructor used — preserve them exactly as given. Label each with the instructor's original framing.)

                ## 5. Edge Cases / Warnings
                (Every edge case, gotcha, common mistake, or warning the instructor mentioned. Include any "be careful about..." or "don't forget..." points.)

                ## 6. Doubt Session Add-ons
                (All concepts, clarifications, or additional examples that came up in the Q&A / doubt session. Treat these as first-class content.)

                ## 7. Final Revision Notes
                (A tight bullet-point cheat sheet covering key formulas, patterns, and interview one-liners from the full lecture.)

                Additional rules for full_clean_notes:
                - Use ## and ### headings exactly as above
                - Use fenced code blocks with language tags: ```java, ```python, etc.
                - Show dry runs step by step with actual values, not just abstract variables
                - Use markdown tables where helpful (e.g. complexity comparison, before/after)
                - Bold key terms and formulas

                The "simple_explanation" field: conversational markdown for a beginner. Use analogies. 2-3 short paragraphs.
                The "revision_notes" field: tight bullet-point cheat sheet for quick revision before an interview (can mirror section 7 of full_clean_notes).
                The "practical_usage" field: real-world problems and patterns where this applies.

                Return ONLY this JSON (no markdown wrapper around the JSON itself):
                {
                  "title": "...",
                  "suggested_module": "...",
                  "full_clean_notes": "...rich markdown string...",
                  "simple_explanation": "...markdown string...",
                  "practical_usage": "...markdown string...",
                  "examples": [
                    {
                      "title": "...",
                      "problem": "...",
                      "input": "...",
                      "approach": "...",
                      "output": "...",
                      "explanation": "..."
                    }
                  ],
                  "mistakes_to_avoid": [
                    {
                      "mistake": "short description of the mistake",
                      "why_it_is_wrong": "...",
                      "correct_approach": "..."
                    }
                  ],
                  "edge_cases": [
                    {
                      "case": "description of the edge case",
                      "handling": "what to do about it"
                    }
                  ],
                  "revision_notes": "...markdown bullet points...",
                  "interview_questions": ["..."],
                  "flashcards": [{"question": "...", "answer": "..."}],
                  "practice_questions": [
                    {
                      "title": "...",
                      "task": "...",
                      "difficulty": "Easy|Medium|Hard",
                      "related_concept": "..."
                    }
                  ],
                  "weak_area_checks": [
                    {
                      "check": "...",
                      "expected_understanding": "..."
                    }
                  ],
                  "extracted_topics": [
                    {
                      "name": "...",
                      "type": "concept|problem|pattern|edge_case|code_pattern|algorithm|data_structure|technique",
                      "importance": "HIGH|MEDIUM|LOW",
                      "coverage_level": "INTRO|INTERMEDIATE|ADVANCED",
                      "summary": "one sentence description",
                      "evidence": "...",
                      "prerequisites": ["..."],
                      "related_topics": ["..."],
                      "confidence": 0,
                      "tree_path": "Scaler DSA > Arrays > Range Queries > Prefix Sum > Range Sum Query"
                    }
                  ]
                }

                Important field rules:
                - type: use only "concept|problem|pattern|edge_case|code_pattern|algorithm|data_structure|technique"
                - importance: return only "HIGH", "MEDIUM", or "LOW"
                - confidence: integer from 0 to 100
                - Structured JSON fields (examples, flashcards, mistakes_to_avoid, etc.) must not be empty arrays if the transcript contains relevant content, even if the same content also appears in full_clean_notes.

                Rules for tree_path — use deep hierarchy, not flat grouping:
                - tree_path is a " > "-delimited string. Use 2-5 segments depending on how specific the topic is.
                - If a topic is a problem or variation solved using a technique, place it UNDER that technique. Do not put it at the same level.
                - If a topic is a subtype or application of another topic in this lecture, it goes under that topic.
                - Avoid flat paths like "Subject > Module > Topic" for everything. Prefer depth where the content warrants it.
                - Do not create two topics with the same meaning (e.g. "Associativity" and "Associative Operations"). Merge them into one.
                - Prefer precise names: use "Prefix Cancellation" instead of "Associativity" for the concept that prefix subtraction requires invertible operations.
                - If no clear hierarchy exists, return "".

                Tree path examples for a Prefix Sum lecture in a DSA course called "Scaler DSA":
                "Scaler DSA > Arrays > Dynamic Arrays"
                "Scaler DSA > Arrays > Range Queries > Prefix Sum"
                "Scaler DSA > Arrays > Range Queries > Prefix Sum > Range Sum Query"
                "Scaler DSA > Arrays > Range Queries > Prefix Sum > Prefix Count"
                "Scaler DSA > Arrays > Range Queries > Prefix Sum > Equilibrium Index"
                "Scaler DSA > Arrays > Range Queries > Prefix Sum > Sum of Even Numbers in Range"
                "Scaler DSA > Arrays > Range Queries > Prefix Sum > Prefix Cancellation"
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

    public String buildTimelinePrompt(TimelineGenerationContext ctx,
                                       LocalDate windowStart,
                                       LocalDate planUntil,
                                       int maxItemsPerDay,
                                       int maxTotalItems,
                                       java.util.Set<Long> alreadyPlannedLectureIds) {
        LearningTarget target = ctx.getTarget();
        long daysRemaining = ChronoUnit.DAYS.between(windowStart, target.getTargetDate());
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        String system = String.format("""
                You are a study planner for Krishna, a software engineer preparing for interviews.

                GENERATE ONLY THESE ITEM TYPES:
                - STUDY_LECTURE: 30-60 min. Direct study of a lecture.
                - REVISION:      20-30 min. Revisit a studied lecture 2-3 days later.
                - PRACTICE:      20-60 min. Problem-solving after completing a topic group.
                Do NOT create ADD_TRANSCRIPT or GENERATE_NOTES items. Assume all material is ready.

                DAILY BUDGET — HARD RULE:
                - Total estimatedMinutes per day MUST be <= the daily budget. Never exceed it.
                - If REVISION or PRACTICE would push a day over budget, skip it.
                - Prefer STUDY_LECTURE over REVISION when time is tight.

                SCHEDULING RULES:
                - Goal: complete as many lectures as possible before the deadline.
                - Lectures marked already-planned are in an earlier window — do not schedule them again.
                - Generate output only for the requested plan window. Do not output dates outside it.
                - HARD LIMIT: maximum %d items per day.
                - HARD LIMIT: maximum %d items in the full response.
                - Lectures with the same topic_group are related — schedule consecutively where possible.
                - Return every date in the plan window exactly once.
                - Never use guilt or shame language.

                REVISION: after studying a lecture, schedule one REVISION 2-3 days later (20-30 min).
                PRACTICE: after 3+ lectures from the same topic_group, add one PRACTICE block.
                lectureId: use the numeric lecture id for STUDY_LECTURE; null for REVISION and PRACTICE.

                Return STRICT valid JSON only. No markdown. No explanation. No trailing commas.
                """, maxItemsPerDay, maxTotalItems);

        StringBuilder user = new StringBuilder();
        user.append("Today: ").append(windowStart.format(fmt)).append("\n");
        user.append("Deadline: ").append(target.getTargetDate().format(fmt)).append("\n");
        user.append("Days remaining: ").append(daysRemaining).append("\n");
        user.append("Plan window: ").append(windowStart.format(fmt)).append(" to ").append(planUntil.format(fmt)).append("\n");
        user.append("Daily budget: ").append(target.getDailyMinutes()).append(" min\n");
        user.append("Max items per day: ").append(maxItemsPerDay).append("\n");
        user.append("Max total items: ").append(maxTotalItems).append("\n\n");

        user.append("Important:\n");
        user.append("You are receiving all lectures for curriculum context.\n");
        user.append("Use the full list for topic ordering and grouping.\n");
        user.append("Generate output only for the plan window.\n");
        user.append("Do not output dates after ").append(planUntil.format(fmt)).append(".\n");
        user.append("Do not exceed ").append(maxTotalItems).append(" total items.\n");
        user.append("Do not create many small prep tasks just to fill time.\n\n");

        Map<Long, String> groupMap = ctx.getLectureGroupMap() != null ? ctx.getLectureGroupMap() : Map.of();
        user.append("Lectures (id | title | module | topic_group | status | content_status | estimated_minutes | already-planned):\n");
        for (Lecture l : interleaveByModule(ctx.getLectures())) {
            String group = groupMap.getOrDefault(l.getId(), "-");
            String planned = alreadyPlannedLectureIds.contains(l.getId()) ? "yes" : "no";
            user.append(String.format("  %d | %s | %s | %s | %s | %s | %d min | %s%n",
                    l.getId(),
                    l.getTitle(),
                    l.getModuleName() != null ? l.getModuleName() : "-",
                    group,
                    l.getStatus().name(),
                    l.getContentStatus() != null ? l.getContentStatus() : "NOT_ADDED",
                    l.getEstimatedMinutes() != null ? l.getEstimatedMinutes() : 60,
                    planned));
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

                Return ONLY valid JSON. No markdown. No explanation. No comments inside JSON.
                Do not exceed the daily budget per day.
                Do not create ADD_TRANSCRIPT or GENERATE_NOTES.
                {
                  "days": [
                    {
                      "date": "YYYY-MM-DD",
                      "items": [
                        {
                          "itemType": "STUDY_LECTURE | REVISION | PRACTICE",
                          "title": "short title max 6 words",
                          "estimatedMinutes": 20-60,
                          "lectureId": "copy exact lecture id number or null"
                        }
                      ]
                    }
                  ]
                }
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

    private List<Lecture> interleaveByModule(List<Lecture> lectures) {
        // Group lectures by module, preserving insertion (lecture) order within each module
        Map<String, List<Lecture>> byModule = new LinkedHashMap<>();
        for (Lecture l : lectures) {
            String key = l.getModuleName() != null ? l.getModuleName() : "";
            byModule.computeIfAbsent(key, k -> new ArrayList<>()).add(l);
        }
        // Round-robin across modules so the AI sees one from each module before seeing a second
        List<List<Lecture>> groups = new ArrayList<>(byModule.values());
        List<Lecture> result = new ArrayList<>(lectures.size());
        int maxSize = groups.stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < maxSize; i++) {
            for (List<Lecture> group : groups) {
                if (i < group.size()) result.add(group.get(i));
            }
        }
        return result;
    }
}
