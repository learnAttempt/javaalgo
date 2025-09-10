package tree;

import java.util.*;
//import javafx.util.Pair;

class TreeNode{
    int val;
    TreeNode left,right;
    TreeNode(int val){
        this.val=val;
        left=right=null;
    }
}
public class VerticalOrderTraversal {

    public static void main(String[] args) {
        VerticalOrderTraversal v = new VerticalOrderTraversal();
        TreeNode root = new TreeNode(3);
        root.left=new TreeNode(9);
        root.right=new TreeNode(20);
        root.right.left=new TreeNode(15);
        root.right.right =new TreeNode(7);
      //  v.verticalOrder(root).forEach( System.out::println);
        v.verticalOrder1(root).forEach(System.out::println);
    }


   /* private List<List<Integer>> getVerticalOrder(TreeNode root) {
        if (root == null)
            return new ArrayList<>();

        List<List<Integer>> output=new ArrayList<>();
        HashMap<Integer, List<Integer>> hm = new HashMap<>();
        Queue<Pair<TreeNode, Integer>> queue = new LinkedList<>();
        queue.offer(new Pair(root,0));
        List<Integer> list=new ArrayList<>();
        list.add(root.val);
        hm.put(0,list);
        int column=0;
        int minC=0,maxC=0;

        while(!queue.isEmpty()){

            Pair<TreeNode,Integer> ele=queue.poll();
            column=ele.getValue();
            if(ele.getKey()!=null) {
                hm.getOrDefault(column, new ArrayList<>()).add(ele.getKey().val);
                queue.offer(new Pair(root.left,column-1));
                queue.offer(new Pair(root.right,column+1));

            }
            minC=Math.min(minC,column);
            maxC=Math.max(maxC,column);


        }

        for(int i=minC;i<=maxC;i++){
            output.add(hm.get(i));
        }


return output;


    }*/


    public List<List<Integer>> verticalOrder(TreeNode root) {
        return new java.util.AbstractList(){
            List<List<Integer>> result;
            Map<Integer, List<Integer>> map;
            int min = 0;
            int max = 0;
            private void init(){
                result = new ArrayList();
                map = new HashMap();
                if(root == null){
                    return;
                }
                bfs();
                processList();
            }

            private void bfs(){
                ArrayDeque<NodeLevel> queue = new ArrayDeque();
                queue.offer(new NodeLevel(0, root));
                while(!queue.isEmpty()){
                    NodeLevel nodeLevel = queue.poll();
                    List<Integer> list = map.getOrDefault(nodeLevel.level, new ArrayList());
                    list.add(nodeLevel.node.val);
                    map.put(nodeLevel.level, list);
                    if(nodeLevel.node.left != null){
                        queue.offer(new NodeLevel(nodeLevel.level - 1, nodeLevel.node.left));
                    }
                    if(nodeLevel.node.right != null){
                        queue.offer(new NodeLevel(nodeLevel.level + 1, nodeLevel.node.right));
                    }
                    min = Math.min(min, nodeLevel.level);
                    max = Math.max(max, nodeLevel.level);
                }
            }

            private void processList(){
                while(min <= max){
                    result.add(map.get(min));
                    min++;
                }
            }

            @Override
            public int size(){
                if(result == null){
                    init();
                }
                return result.size();
            }

            @Override
            public List<Integer> get(int position){
                return result.get(position);
            }
        };
    }

    class NodeLevel{
        int level;
        TreeNode node;
        public NodeLevel(int level, TreeNode node){
            this.level = level;
            this.node = node;
        }
    }
    public List<List<Integer>> verticalOrder1(TreeNode root) {

    List<List<Integer>> result = new ArrayList<>();

    //base condition
    if(root==null) return result;

    //to keep keys sorted
    Map<Integer, List<Integer>> map = new TreeMap<>();
    Queue<TreeNode> q = new LinkedList<>();
    Queue<Integer> colm = new LinkedList<>();

    //add root node to the queue & assign colm 0
    q.add(root);
    colm.add(0);

    int min = 0;
    int max = 0;

    while(!q.isEmpty()){
        TreeNode n = q.poll();
        int col = colm.poll();

        //map.putIfAbsent(col, new ArrayList<Integer>());
        if(!map.containsKey(col)){
            map.put(col, new ArrayList<Integer>());
        }

        map.get(col).add(n.val);

        //left node
        if(n.left!=null){
            q.add(n.left);
            colm.add(col-1);
            min = min<col-1 ? min : col-1;
        }

        //right node
        if(n.right!=null){
            q.add(n.right);
            colm.add(col+1);
            max=max>col+1 ? max : col+1;
        }
    }

    //
    for(int i = min; i<=max; i++){
        result.add(map.get(i));
    }
       return result;
}
}
