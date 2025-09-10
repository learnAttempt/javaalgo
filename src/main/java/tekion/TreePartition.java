package tekion;

public class TreePartition {
    TreeNode head=null;
    Boolean result=false;


    public boolean checkEqualTree(TreeNode root){
        int sum=sum(root);
        head=root;
        if(sum%2!=0)
            return false;
        checkPartition(root,sum);
        return result;
    }

    public int checkPartition(TreeNode root,int total){
        if(root==null)
            return 0;
        int leftSum=checkPartition(root.left,total);
        int rightSum=checkPartition(root.right,total);
        int currentSum=root.val+leftSum+rightSum;
        if(2*currentSum==total && head!=root){
            result=true;
            return 0;
        }
        return  currentSum;
    }
    public int sum(TreeNode root){
        if(root==null)
            return 0;
        return root.val+sum(root.left)+sum(root.right);
    }
    public static void main(String[] args){
        TreeNode root=new TreeNode(5);
        root.left=new TreeNode(10);
        root.right=new TreeNode(10);
        root.right.left= new TreeNode(2);
        root.right.right=new TreeNode(3);
        TreePartition tp=new TreePartition();
        System.out.println(tp.checkEqualTree(root));

    }
//i ->board
    //j-> painter
    // dp[i][j]= min(dp[i][j],max(dp[i+1[j-1],sum(i,k))-> k->i->n
}
