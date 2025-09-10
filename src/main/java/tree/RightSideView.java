package tree;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class RightSideView {

    public static void main(String [] args){
        RightSideView rs=new RightSideView();
        TreeNode root=new TreeNode(1);
        root.left=new TreeNode(2);
        root.right=new TreeNode(3);
        root.left.right=new TreeNode(5);
        root.right.right=new TreeNode(4);
       List<Integer>result= rs.rightSideView(root);
       result.forEach(System.out::println);
    }
    public List<Integer> rightSideView(TreeNode root) {
        if(root==null)
            return new ArrayList<>();
        List<Integer> output=new ArrayList<>();
        Queue<TreeNode> queue=new LinkedList<>();
        queue.offer(root);
        while(!queue.isEmpty()){

            for(int i=0;i<queue.size();i++){
                TreeNode curr=queue.poll();
                if(i==0){
                    output.add(curr.val);
                }
                if(curr.right!=null){
                    queue.offer(curr.right);
                }
                if(curr.left!=null){
                    queue.offer(curr.left);
                }
            }

        }
        return output;
    }
}
