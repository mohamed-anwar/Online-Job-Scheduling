import java.util.ArrayList;

class State {
    public int stateId = 0;
    public int jobId = 0;


    State(int id) {
        stateId = id;
    }

    State(int id, int jid) {
        stateId = id;
        jobId = jid;
    }
}

public class Machine {
    public static int count = 0;
    public int id;

    public State currentState;
    public ArrayList<State> transitions;
    public int makespan = 0;    /* Total schedule wait time = Max job wait time */

    // For benchmarking
    public int jobsCount = 0;
    public int averageJobWaitTime = 0;
    ////////////////////

    Machine() {
        id = count++;
        currentState = new State(0);
        transitions = new ArrayList<State>();

        if (Parameters.debug)
            System.out.printf("Initalized Machine M%d\n", id);
    }

    public void insertState(int i, State s) {
        if (i == transitions.size())
            transitions.add(s);
        else
            transitions.add(i, s);

    }

    public void dump() {
        System.out.printf("Machine M%d (Current State: %d, Makespan: %d, Carried Jobs: %d, Average Wait Time: %d)\n", id, currentState.stateId, makespan, jobsCount, averageJobWaitTime);
        for (int i = 0; i < transitions.size(); ++i) {
            System.out.printf("#%d State: %d (Job %d)\n", i, transitions.get(i).stateId, transitions.get(i).jobId);
        }
    }
}
