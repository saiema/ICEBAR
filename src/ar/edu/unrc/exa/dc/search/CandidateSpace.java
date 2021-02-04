package ar.edu.unrc.exa.dc.search;

import java.util.*;

public class CandidateSpace {

    private final boolean usePriority;
    private final boolean useQueue;
    private final Map<Integer, Stack<FixCandidate>> priorityStack;
    private final Map<Integer, Queue<FixCandidate>> priorityQueue;
    private static final int NON_PRIORITY_INDEX = 0;

    public static CandidateSpace normalStack() {
        return new CandidateSpace(false, false);
    }

    public static CandidateSpace priorityStack() {
        return new CandidateSpace(true, false);

    }

    public static CandidateSpace normalQueue() {
        return new CandidateSpace(false, true);
    }

    public static CandidateSpace priorityQueue() {
        return new CandidateSpace(true, true);
    }

    private CandidateSpace(boolean usePriority, boolean useQueue) {
        this.usePriority = usePriority;
        this.useQueue = useQueue;
        if (useQueue) {
            priorityStack = null;
            priorityQueue = new HashMap<>();
        } else {
            priorityStack = new HashMap<>();
            priorityQueue = null;
        }
    }

    public boolean isEmpty() {
        if (useQueue) {
            assert priorityQueue != null;
            for (Queue<FixCandidate> queue : priorityQueue.values()) {
                if (!queue.isEmpty())
                    return false;
            }
        } else {
            assert priorityStack != null;
            for (Stack<FixCandidate> stack : priorityStack.values()) {
                if (!stack.isEmpty())
                    return false;
            }
        }
        return true;
    }

    public void push(FixCandidate candidate) {
        if (useQueue) {
            pushToQueue(candidate);
        } else {
            pushToStack(candidate);
        }
    }

    private void pushToStack(FixCandidate candidate) {
        if (useQueue)
            throw new IllegalStateException("Calling pushToStack when working with queues");
        int priority;
        if (usePriority) {
            priority = candidate.repairedProperties();
        } else {
            priority = NON_PRIORITY_INDEX;
        }
        assert priorityStack != null;
        Stack<FixCandidate> stack = priorityStack.getOrDefault(priority, new Stack<>());
        stack.push(candidate);
        priorityStack.put(priority, stack);
    }

    private void pushToQueue(FixCandidate candidate) {
        if (!useQueue)
            throw new IllegalStateException("Calling pushToQueue when working with stacks");
        int priority;
        if (usePriority) {
            priority = candidate.repairedProperties();
        } else {
            priority = NON_PRIORITY_INDEX;
        }
        assert priorityQueue != null;
        Queue<FixCandidate> queue = priorityQueue.getOrDefault(priority, new LinkedList<>());
        queue.add(candidate);
        priorityQueue.put(priority, queue);
    }

    public FixCandidate pop() {
        if (useQueue)
            return popFromQueue();
        else
            return popFromStack();
    }

    private FixCandidate popFromStack() {
        if (useQueue)
            throw new IllegalStateException("Calling popFromStack when working with queues");
        int priority;
        assert priorityStack != null;
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

    private FixCandidate popFromQueue() {
        if (!useQueue)
            throw new IllegalStateException("Calling popFromQueue when working with stacks");
        int priority;
        assert priorityQueue != null;
        if (usePriority) {
            priority = priorityQueue.entrySet().stream().filter(integerStackEntry -> !integerStackEntry.getValue().isEmpty()).map(Map.Entry::getKey).max(Comparator.comparingInt(o -> o)).orElse(NON_PRIORITY_INDEX);
        } else {
            priority = NON_PRIORITY_INDEX;
        }
        if (!priorityQueue.containsKey(priority))
            throw new IllegalStateException("Empty queue");
        Queue<FixCandidate> queue = priorityQueue.get(priority);
        if (queue.isEmpty())
            throw new IllegalStateException("Empty queue");
        return queue.poll();
    }

}
