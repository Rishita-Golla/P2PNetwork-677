import java.io.*;
import java.util.*;

public class GenerateFiles {
    public static void createNetworkSetUp(int N) throws IOException {

        System.out.println("Creating the new network setup..");
        HashMap<Integer,String> map = new HashMap();
        HashMap<Integer, List<Integer>> neighbours = new HashMap();
        StringBuffer sb = new StringBuffer("http://127.0.0.1:");
        int portNumber = 5000;

        //create common URL map
        for(int i=1;i<=N;i++)
        {
            map.put(i, sb.toString()+String.valueOf(portNumber));
            portNumber++;
        }
        //create ring topology among the peers
        for(int i=1;i<=N;i++) {
            if(i==1){
                neighbours.put(i, new ArrayList<>(Arrays.asList(N,i+1)));
            }else if(i==N){
                neighbours.put(i, new ArrayList<>(Arrays.asList(i-1,1)));
            }else{
                neighbours.put(i, new ArrayList<>(Arrays.asList(i-1,i+1)));
            }
        }
//
//        File file = new File("src/common.txt");
//        File file1 = new File("src/buyer.txt");
//        File file2 = new File("src/seller.txt");
//        File file3 = new File("src/commands.txt");

        File file = new File("common.txt");
        File file1 = new File("buyer.txt");
        File file2 = new File("seller.txt");
        File file3 = new File("commands.txt");
        BufferedWriter bf = new BufferedWriter( new FileWriter(file));
        BufferedWriter bf1 = new BufferedWriter( new FileWriter(file1));
        BufferedWriter bf2 = new BufferedWriter( new FileWriter(file2));
        BufferedWriter bf3 = new BufferedWriter( new FileWriter(file3));

        //ensure network has at least 1 buyer and 1 seller
        bf.write(1 + "=" + map.get(1));
        bf.write(",buyer");
        bf.newLine();
        bf3.write("java Client 1 common.txt buyer.txt fish");
        bf3.newLine();
        bf.write(2 + "=" + map.get(2));
        bf.write(",seller");
        bf.newLine();
        bf3.write("java Server 2 common.txt seller.txt fish");
        bf1.write(1+"="+neighbours.get(1).toString());
        bf1.newLine();
        bf2.write(2+"="+neighbours.get(2).toString());
        bf2.newLine();
        bf3.newLine();

        String[] itemName = {"fish","salt","boar"};

        if(N > 2) {
            int role_index;
            int count =0;
            Random rand = new Random();
            for(int index=3;index<=N;index++) {
                role_index = rand.nextInt(2);
                bf.write(index + "=" + map.get(index));
                if(role_index == 0){
                    bf.write(",buyer");
                    bf1.write(index+"="+neighbours.get(index).toString());
                    bf1.newLine();
                    bf3.write("java Client "+index+" common.txt buyer.txt "+itemName[(count)%3]);
                    bf3.newLine();
                }else{
                    bf.write(",seller");
                    bf2.write(index+"="+neighbours.get(index).toString());
                    bf2.newLine();
                    bf3.write("java Server "+index+" common.txt seller.txt "+itemName[(count)%3]);
                    bf3.newLine();
                }
                count++;
                bf.newLine();
            }

        }
        bf.flush();
        bf.close();
        bf1.flush();
        bf1.close();
        bf2.flush();
        bf2.close();
        bf3.flush();
        bf3.close();

        System.out.println("Node IDs and their URLs: "+map);
        System.out.println("****************");
        System.out.println("Node IDs and their Neighbours: "+neighbours);

    }

    public static void main(String[] args) {
        int N = Integer.parseInt(args[0]);
        try{
            if(N<2){
                System.out.println("Minimum 2 nodes are required to get the bazaar running.");
                return;
            }
            createNetworkSetUp(N);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}