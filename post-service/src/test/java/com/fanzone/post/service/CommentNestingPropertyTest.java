package com.fanzone.post.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based tests for comment nesting depth (Property 7).
 * <p>
 * Verifies that nesting level never exceeds 3, regardless of how many
 * levels of replies are attempted.
 */
class CommentNestingPropertyTest {

    @Property(tries = 500)
    void nestingLevel_neverExceedsMax(@ForAll @IntRange(min = 0, max = 100) int parentLevel) {
        int result = CommentServiceImpl.computeReplyNestingLevel(parentLevel);
        assert result <= CommentServiceImpl.getMaxNestingLevel() :
                "Nesting level " + result + " exceeds max " + CommentServiceImpl.getMaxNestingLevel()
                        + " (parent was level " + parentLevel + ")";
    }

    @Property(tries = 200)
    void nestingLevel_incrementsWhenBelowMax(@ForAll @IntRange(min = 0, max = 2) int parentLevel) {
        int result = CommentServiceImpl.computeReplyNestingLevel(parentLevel);
        assert result == parentLevel + 1 :
                "Expected level " + (parentLevel + 1) + " but got " + result + " for parent level " + parentLevel;
    }

    @Property(tries = 200)
    void nestingLevel_clampedAtMax(@ForAll @IntRange(min = 3, max = 100) int parentLevel) {
        int result = CommentServiceImpl.computeReplyNestingLevel(parentLevel);
        assert result == CommentServiceImpl.getMaxNestingLevel() :
                "Expected max level " + CommentServiceImpl.getMaxNestingLevel()
                        + " but got " + result + " for parent level " + parentLevel;
    }

    @Property(tries = 100)
    void nestingLevel_chainedReplies_neverExceedsMax(@ForAll @IntRange(min = 1, max = 50) int chainDepth) {
        // Simulate creating a chain of replies
        int currentLevel = 0;
        for (int i = 0; i < chainDepth; i++) {
            currentLevel = CommentServiceImpl.computeReplyNestingLevel(currentLevel);
        }
        assert currentLevel <= CommentServiceImpl.getMaxNestingLevel() :
                "After " + chainDepth + " chained replies, level is " + currentLevel
                        + " which exceeds max " + CommentServiceImpl.getMaxNestingLevel();
    }

    @Example
    void nestingLevel_exactBoundary_level2_gives3() {
        assert CommentServiceImpl.computeReplyNestingLevel(2) == 3;
    }

    @Example
    void nestingLevel_exactBoundary_level3_stays3() {
        assert CommentServiceImpl.computeReplyNestingLevel(3) == 3;
    }

    @Example
    void nestingLevel_level0_gives1() {
        assert CommentServiceImpl.computeReplyNestingLevel(0) == 1;
    }
}
