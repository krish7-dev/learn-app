package com.learnhowyoulearn.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "openai.enabled", havingValue = "false")
@Slf4j
public class MockAiClient implements AiClient {

    private static final String MOCK_NOTES_RESPONSE = """
            {
              "title": "[MOCK] Arrays Basics",
              "full_clean_notes": "Arrays store elements in contiguous memory. Access is O(1) by index.",
              "simple_explanation": "Think of an array like numbered boxes in a row. You can jump to any box instantly if you know its number.",
              "practical_usage": "Use arrays when you need fast random access and the size is known upfront.",
              "examples": [
                {"title": "Two Sum using HashMap", "code": "Map<Integer,Integer> map = new HashMap<>();\\nfor(int i=0;i<nums.length;i++){\\n  int diff = target-nums[i];\\n  if(map.containsKey(diff)) return new int[]{map.get(diff),i};\\n  map.put(nums[i],i);\\n}", "explanation": "Store seen elements in map, check complement on each step."}
              ],
              "mistakes_to_avoid": ["Off-by-one errors on boundaries","Forgetting to handle empty arrays"],
              "edge_cases": ["Empty array","Single element","All same elements","Sorted vs unsorted"],
              "revision_notes": "Key patterns: prefix sum, sliding window, two pointers, kadane.",
              "interview_questions": ["Find max subarray sum (Kadane)","Two Sum","Move Zeros"],
              "flashcards": [
                {"front": "What is time complexity of array access?", "back": "O(1)"},
                {"front": "When to use prefix sum?", "back": "Range sum queries, subarray sum problems"}
              ],
              "practice_questions": ["Subarray Sum Equals K","Maximum Product Subarray"],
              "weak_area_checks": ["Can you dry-run prefix sum on [1,2,3,4]?","What breaks the sliding window approach?"],
              "extracted_topics": [
                {"name": "Arrays", "importance": "HIGH", "coverage_level": "INTERMEDIATE", "evidence": "Core lecture topic"},
                {"name": "Prefix Sum", "importance": "HIGH", "coverage_level": "INTRO", "evidence": "Mentioned in revision notes"}
              ]
            }
            """;

    @Override
    public AiResponse generate(AiRequest request) {
        log.warn("[MOCK] AI call for purpose={} — returning stub response (OPENAI_ENABLED=false)", request.getPurpose());
        return AiResponse.builder()
                .content(MOCK_NOTES_RESPONSE)
                .model("mock")
                .latencyMs(50)
                .rawResponse(MOCK_NOTES_RESPONSE)
                .build();
    }
}
