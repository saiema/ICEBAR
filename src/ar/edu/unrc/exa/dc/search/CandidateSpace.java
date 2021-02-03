package ar.edu.unrc.exa.dc.search;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class CandidateSpace {

    private final boolean usePriority;
    private final Map<Integer, Stack<FixCandidate>> priorityStack;
    private static final int NON_PRIORITY_INDEX = 0;

    public static CandidateSpace normalStack() {
        return new CandidateSpace(false);
    }

    public static CandidateSpace priorityStack() {
        return new CandidateSpace(true);
    }

    private CandidateSpace(boolean usePriority) {
        this.usePriority = usePriority;
        priorityStack = new HashMap<>();
        if (!usePriority)
            priorityStack.put(NON_PRIORITY_INDEX, new Stack<>());
    }

    public boolean isEmpty() {
        if (usePriority) {
            for (Stack<FixCandidate> stack : priorityStack.values()) {
                if (!stack.isEmpty())
                    return false;
            }
        } else {
            return priorityStack.get(NON_PRIORITY_INDEX).isEmpty();
        }
        return true;
    }

    public void push(FixCandidate candidate) {
        int priority;
        if (usePriority) {
            priority = candidate.repairedProperties();
        } else {
            priority = NON_PRIORITY_INDEX;
        }
        Stack<FixCandidate> stack = priorityStack.getOrDefault(priority, new Stack<>());
        stack.push(candidate);
        priorityStack.put(priority, stack);
    }

    public FixCandidate pop() {
        int priority;
        if (usePriority) {
            priority = priorityStack.entrySet().stream().filter(integerStackEntry -> !integerStackEntry.getValue().isEmpty()).map(Map.Entry::getKey).max(Comparator.comparingInt(o -> o)).orElse(NON_PRIORITY_INDEX);
        } else {
            priority = NON_PRIORITY_INDEX;
        }
        if (!priorityStack.containsKey(priority))
            throw new IllegalStateException("Empty stack");
        Stack<FixCandidate> stack = priorityStack.get(priority);
        if (stack.isEmpty())
            throw new IllegalStateException("Empty stack");
        return stack.pop();
    }

}
