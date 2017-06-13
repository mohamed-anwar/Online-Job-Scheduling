import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class Job {
    public static int count = 0;
    public int id = 0;

    public State initState;
    public State finalState;

    Job(int i, int f) {
        id = count++;
        initState = new State(i, id);
        finalState = new State(f, id);
    }
}

public class Scheduler {
    public static int waitTime(State s1, State s2) {
        if (s1.stateId == s2.stateId)
            return 0;

        int constTime = Parameters.constTime;
        int sweepTime = Parameters.sweepTime;
        int sweepedStates = (s1.stateId > s2.stateId? 1 : -1) * (s1.stateId - s2.stateId);
        return constTime + sweepedStates * sweepTime;
    }

    public static int[][] buildCache(Machine m, State s) {
        int len = m.transitions.size();
        int[][] cache = new int[len+1][2];

        cache[0][0] = 0;
        cache[0][1] = waitTime(m.currentState, s);
        cache[0][1] += waitTime(s, m.transitions.get(0));

        for (int i = 1; i < len; ++i) {
            State prev = m.transitions.get(i-1);
            State next = m.transitions.get(i);

            cache[i][0] = i;
            cache[i][1] = waitTime(prev, s);
            cache[i][1] += waitTime(s, next);
            cache[i][1] -= waitTime(prev, next);
        }

        cache[len][0] = len;
        cache[len][1] = waitTime(m.transitions.get(len-1), s);

        Arrays.sort(cache, new Comparator<int[]>() {
            @Override
            public int compare(final int[] a, final int[] b) {
                return a[1] - b[1];
            }
        });

        return cache;
    }

    static class InsertCost {
        int initStateIdx = 0;
        int finalStateIdx = 0;
        int makespan = 0;

        InsertCost(int i, int f, int m) {
            initStateIdx = i;
            finalStateIdx = f;
            makespan = m;
        }
    }

    public static InsertCost findMinMakespan(Machine m, Job job, int thresholdTime) {
        //System.out.printf("findMinMakespan(M%d, J(%d, %d), %d)\n", m.id, job.initState.stateId, job.finalState.stateId, thresholdTime);
       
        if (m.transitions.size() != 0) {
            InsertCost cost = new InsertCost(0, 0, Integer.MAX_VALUE);
            int transitionsCount = m.transitions.size();

            int waitTimeInitFinal = waitTime(job.initState, job.finalState);

            // Try inserting init and final states subsequently only if 
            // the transition time does not impact makespan higher than
            // threshold time
            if (m.makespan + waitTimeInitFinal < thresholdTime) {
                int makespanImpact = 0;

                // Try inserting before all transitions
                makespanImpact  = waitTime(m.currentState, job.initState);
                makespanImpact += waitTimeInitFinal;
                makespanImpact += waitTime(job.finalState, m.transitions.get(0));

                if (m.makespan + makespanImpact < cost.makespan) {
                    cost.initStateIdx = 0;
                    cost.finalStateIdx = 0;
                    cost.makespan = m.makespan + makespanImpact;
                }

                // Try inserting before State i
                for (int i = 1; i < transitionsCount - 1; ++i) {
                    State prev = m.transitions.get(i-1);
                    State next = m.transitions.get(i);

                    makespanImpact  = waitTime(prev, job.initState);
                    makespanImpact += waitTimeInitFinal;
                    makespanImpact += waitTime(job.finalState, next);

                    if (m.makespan + makespanImpact < cost.makespan) {
                        cost.initStateIdx = i;
                        cost.finalStateIdx = i;
                        cost.makespan = m.makespan + makespanImpact;
                    }
                }

                // Try inserting after all transitions
                makespanImpact  = waitTime(m.transitions.get(transitionsCount-1), job.initState);
                makespanImpact += waitTimeInitFinal;

                if (m.makespan + makespanImpact < cost.makespan) {
                    cost.initStateIdx = transitionsCount;
                    cost.finalStateIdx = transitionsCount;
                    cost.makespan = m.makespan + makespanImpact;
                }
            }

            // Build cache of insertions of init and final states in transitions
            int[][] initCache = buildCache(m, job.initState);
            int[][] finalCache = buildCache(m, job.finalState);

            // Find minimum non-subsequent states (insert before i and j)
            for (int i = 0; i < initCache.length; ++i) {
                boolean found = false;

                for (int j = 0; j < finalCache.length; ++j) {
                    if (initCache[i][0] < finalCache[j][0]) {   // If init state id is less than final state id
                        int makespanImpact = initCache[i][1] + finalCache[j][1];

                        if (m.makespan + makespanImpact < cost.makespan) {
                            cost.initStateIdx = i;
                            cost.finalStateIdx = j;
                            cost.makespan = m.makespan + makespanImpact;
                        }

                        found = true;
                        break;
                    }
                }

                if (found)
                    break;
            }

            return cost;
        } else {    /* No jobs assigned */
            return new InsertCost(0, 0, waitTime(m.currentState, job.initState) + waitTime(job.initState, job.finalState));
        }
    }

    public static void scheduleJob(Machine[] machines, Job job) {
        if (Parameters.debug)
            System.out.printf("Scheduling Job %d -> %d\n", job.initState.stateId, job.finalState.stateId);

        Machine selectedMachine = null;
        InsertCost minimumCost = new InsertCost(0, 0, Integer.MAX_VALUE);

        for (Machine m: machines) {
            InsertCost cost = findMinMakespan(m, job, minimumCost.makespan);

            if (cost.makespan < minimumCost.makespan) {
                minimumCost = cost;
                selectedMachine = m;
            }
        }

        if (Parameters.debug)
            System.out.printf("Selected machine M%d (%d -> %d: %d)\n", selectedMachine.id, minimumCost.initStateIdx, minimumCost.finalStateIdx, minimumCost.makespan);

        selectedMachine.insertState(minimumCost.finalStateIdx, job.finalState);
        selectedMachine.insertState(minimumCost.initStateIdx, job.initState);
        selectedMachine.makespan = minimumCost.makespan;

        if (Parameters.benchmark) {
            int jobWaitTime = 0;

            State prev = selectedMachine.currentState;
            for (int i = 0; i < minimumCost.finalStateIdx + 1; ++i) {
                State current = selectedMachine.transitions.get(i);
                jobWaitTime += waitTime(prev, current);
                prev = current;
            }

            int currentAverage = selectedMachine.averageJobWaitTime;
            int jobsCount = selectedMachine.jobsCount;

            selectedMachine.averageJobWaitTime = (currentAverage * jobsCount + jobWaitTime) / (jobsCount + 1);
            selectedMachine.jobsCount += 1;
        }
    }

    public static void main(String[] args) {
        final int machinesCount = 4;

        Machine[] machines = new Machine[machinesCount];

        for (int i = 0; i < machines.length; ++i)
            machines[i] = new Machine();

        for (int i = 0; i < 10; ++i) {
            int Si = (int) (Math.random() * 10);
            int Sf = (int) (Math.random() * 10);
            while (Sf == Si) Sf = (int) (Math.random() * 10);

            scheduleJob(machines, new Job(Si, Sf));
        }

        // For benchmarking
        int avg = 0;
        int jobs = 0;
        //////////////////

        for (int i = 0; i < machines.length; ++i) {
            machines[i].dump();

            if (Parameters.benchmark) {
                int cJobs = jobs + machines[i].jobsCount;
                avg = (avg * jobs + machines[i].averageJobWaitTime * machines[i].jobsCount) / cJobs;
                jobs = cJobs;
            }
        }

        if (Parameters.benchmark) {
            System.out.printf("Total Carried Jobs: %d\n", jobs);
            System.out.printf("Overall Average Job Wait Time: %d\n", avg);
        }
    }
}
