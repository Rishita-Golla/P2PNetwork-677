import java.io.*;
import java.util.*;

public class GenerateFiles {
    public static void createNetworkSetUp(int N) throws IOException {

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
        //D:\Juie Darwade\University of Massachusetts Amherst\677\P2PNetwork_backup\src\seller.properties
        System.out.println("map"+map);
        File file = new File("src/commonConfig.txt");
        BufferedWriter bf = new BufferedWriter( new FileWriter(file));
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            // put key and value separated by a colon
            bf.write(entry.getKey() + "=" + entry.getValue());
            // new line
            bf.newLine();
        }
        bf.flush();
        bf.close();

        for(int i=1;i<=N;i++) {
            if(i==1){
                neighbours.put(i, new ArrayList<>(Arrays.asList(N,i+1)));
            }else if(i==N){
                neighbours.put(i, new ArrayList<>(Arrays.asList(i-1,1)));
            }else{
                neighbours.put(i, new ArrayList<>(Arrays.asList(i-1,i+1)));
            }
        }
        System.out.println("neigbours"+neighbours);

        File file1 = new File("src/buyerConfig.txt");
        File file2 = new File("src/sellerConfig.txt");
        BufferedWriter bf1 = new BufferedWriter( new FileWriter(file1));
        BufferedWriter bf2 = new BufferedWriter( new FileWriter(file2));

        int role_index;
        Random rand = new Random();
        for(int index=1;index<=N;index++)
        {
            if(index==1) {
                bf1.write(index+"="+neighbours.get(index).toString().replaceAll("\\[", "").replaceAll("\\]",""));
                bf1.newLine();
            }else if(index==2) {
                bf2.write(index+"="+neighbours.get(index).toString().replaceAll("\\[", "").replaceAll("\\]",""));
                bf2.newLine();
            }else{
                role_index = rand.nextInt(2);
                if(role_index == 0){ //buyer
                    bf1.write(index+"="+neighbours.get(index).toString().replaceAll("\\[", "").replaceAll("\\]",""));
                    bf1.newLine();
                }else{ //seller
                    bf2.write(index+"="+neighbours.get(index).toString().replaceAll("\\[", "").replaceAll("\\]",""));
                    bf2.newLine();
                }
            }
        }
        bf1.flush();
        bf1.close();
        bf2.flush();
        bf2.close();

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