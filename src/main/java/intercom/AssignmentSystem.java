package intercom;
/*
# In this exercise, you’ll build a system to assign conversations to customer support agents.

# Task
# Implement an AssignmentSystem class that has the following API:

        # - init(agent_names)
#   Initializes with a list of available agents

# - set_limit(agent_name, limit)
#   Sets the conversation limit for a specific agent.

# - assign(conversation_ids)
#   Takes a list of conversation IDs, assigns them to the next available agents, and returns their names.

# Requirements
# - When assigning conversations, balance load evenly:
        #   - Assign new conversations to the agent with the fewest conversations
#   - If there are ties, pick the agent who’s been waiting the longest since their last assignment
# - Every agent can only take a limited maximum number of conversations (default is 2)

# Example
# agents = ["Alice", "Bob", "Charlie"]
        # system = AssignmentSystem(agents)

# system.set_limit("Bob", 4)
# system.set_limit("Charlie", 3)

# I want to assign the first 4 conversations
# print(system.assign([1, 2, 3, 4]))
        # Output: ["Alice", "Bob", "Charlie", "Alice"]

        # system.set_limit("Alice", 3)

# I want to assign another 5 conversations
# print(system.assign([5, 6, 7, 8, 9]))
        # Output: ['Bob', 'Charlie', 'Alice', 'Bob', 'Charlie']
        */
        import java.util.*;

class AssignmentSystem{

    private static final int DEFAULT_LIMIT=2;
    private long counter=0;


    private static class Agent{
        String name;
        int assignedCount;
        int limit;
        long lastAssignedTime;
        Agent(String name,int limit){
            this.name=name;
            this.limit=limit;
        }

    }

    private final Map<String,Agent> agents;
    private final PriorityQueue<Agent> heap;

    public AssignmentSystem(){
        this.agents=new HashMap<>();
        this.heap=new PriorityQueue<>((a,b)->{
            if(a.assignedCount!=b.assignedCount)
                return Integer.compare(a.assignedCount, b.assignedCount);
            return Long.compare(a.lastAssignedTime, b.lastAssignedTime);
        });
    }

    public void init(List<String> agentNames){
        if(agentNames.isEmpty())
            throw new IllegalArgumentException("no agents given as input");
        for(String name: agentNames)
            agents.put(name,new Agent(name, DEFAULT_LIMIT));

        heap.addAll(agents.values());
    }

    public void set_limit(String agentName, int limit){
        Agent agent=agents.get(agentName);
        if(agent==null)
            throw new IllegalArgumentException("Unknown agent:"+agentName);
        agent.limit=limit;

        if(agent.assignedCount< agent.limit && !heap.contains(agent))
            heap.add(agent);


    }

    public List<String> assign(List<String> conversationIds){
        List<String> result= new ArrayList<>();
        if(heap.isEmpty()) return result;
        for(String conversation: conversationIds){
            Agent agent=nextAvailableAgent();
            if(agent==null)
                return result;
            result.add(agent.name);
            agent.assignedCount++;
            agent.lastAssignedTime=++counter;

            if(agent.assignedCount<agent.limit)
                heap.offer(agent);
        }
        return result;
    }

    private Agent nextAvailableAgent(){
        while(!heap.isEmpty()){
            Agent top=heap.poll();
            if(top.assignedCount<top.limit)
                return top;
        }
        return null;
    }



}