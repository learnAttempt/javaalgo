package linkedin;

import java.util.*;

public class ShortestDistanceBetweenConnections {
    List<List<Integer>> adjList=new ArrayList<>();

    public void formGraph( int n,int[][] connections) {
        for (int i = 0; i <= n; i++) {
            adjList.add(new ArrayList<>());
        }
            for (int[] edge : connections) {
                adjList.get(edge[0]).add(edge[1]);
                adjList.get(edge[1]).add(edge[0]);
            }


    }

    private int bfs(int source,int target){

        Queue<int[]> beginQueue =new LinkedList<>();
        Queue<int[]> endQueue=new LinkedList<>();
        HashMap<Integer,Integer> visitedBegin=new HashMap<>();
        HashMap<Integer,Integer> visitedEnd=new HashMap<>();
        beginQueue.add(new int[]{source,0});
        endQueue.add(new int[]{target,0});
        visitedBegin.put(source,1);;
        visitedEnd.put(target,1);
        int dist=-1;
        while (!beginQueue.isEmpty() && !endQueue.isEmpty()){
            if(beginQueue.size()<=endQueue.size())
            {
               dist= visitConnection(beginQueue,visitedBegin,visitedEnd);
            }else{
                dist=visitConnection(endQueue,visitedEnd,visitedBegin);
            }
            if(dist>-1)
                return dist;
        }
        return 0;



    }

    private int visitConnection(Queue<int[]> queue,HashMap<Integer,Integer> visited,HashMap<Integer,Integer> otherVisited ){
        for(int i=0;i<queue.size();i++){
            int[] curr=queue.poll();
            for(Integer edge:adjList.get(curr[0])){
                if(otherVisited.containsKey(edge))
                    return curr[1]+otherVisited.get(edge);
                if(!visited.containsKey(edge)){
                    visited.put(edge,curr[1]+1);
                    queue.add(new int[]{edge,curr[1]+1});
                }
            }
        }
        return -1;
    }

    public static void main(String [] args){
        ShortestDistanceBetweenConnections conn =new ShortestDistanceBetweenConnections();
        int n=6;
        int source=1;
        int target=6;
        int [][] connections={{1,2},{2,3},{3,4},{4,5},{5,6}};
        conn.formGraph(n,connections);
       System.out.println( conn.bfs(source,target));
    }
}
