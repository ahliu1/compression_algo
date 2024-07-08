import java.util.PriorityQueue;


/**
* Although this class has a history of several years,
* it is starting from a blank-slate, new and clean implementation
* as of Fall 2018.
* <P>
* Changes include relying solely on a tree for header information
* and including debug and bits read/written information
*
* @author Owen Astrachan
* @author Amy Liu
* Revise
*/


public class HuffProcessor {


   private class HuffNode implements Comparable<HuffNode> {
       HuffNode left; // left child
       HuffNode right; //right child
       int value; // value stored in the node
       int weight; // frequency of the character


       public HuffNode(int val, int count) {
           value = val;
           weight = count;
       }
       public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
           value = val; // char value
           weight = count; // freq count
           left = ltree; // left subtree
           right = rtree; // right subtree
       }


       public int compareTo(HuffNode o) {
           return weight - o.weight;
       }




   }


   public static final int BITS_PER_WORD = 8;
   public static final int BITS_PER_INT = 32;
   public static final int ALPH_SIZE = (1 << BITS_PER_WORD);
   public static final int PSEUDO_EOF = ALPH_SIZE; // special end of file character
   public static final int HUFF_NUMBER = 0xface8200; // magic num for huffman files
   public static final int HUFF_TREE  = HUFF_NUMBER | 1; // identifier for huffman tree


   private boolean myDebugging = false; // debugging flag
  
   public HuffProcessor() {
       this(false); // calls other constructor with debugging off
   }
  
   public HuffProcessor(boolean debug) {
       myDebugging = debug; // sets debugging flag
   }


   /**
    * Compresses a file. Process must be reversible and loss-less.
    *
    * @param in
    *            Buffered bit stream of the file to be compressed.
    * @param out
    *            Buffered bit stream writing to the output file.
    */
   public void compress(BitInputStream in, BitOutputStream out){
       // pseudocode from directions
       int[] counts = getCounts(in); // freq counts of each byte


       //int bits = in.readBits(BITS_PER_WORD);
      
       //while (bits != -1){
       //  counts[bits]++;
       //  bits = in.readBits(BITS_PER_WORD);
       //}




       //counts[PSEUDO_EOF] = 1;


       HuffNode root = makeTree(counts); // make Huffman tree from frequency counts


       in.reset(); // reset input stream, read from beginning
      
       out.writeBits(BITS_PER_INT, HUFF_TREE); // write magic num and tree header to output file
       writeTree(root, out);

       String[] encodings = new String[ALPH_SIZE + 1]; // encodings for each byte
       makeEncodings(root, "", encodings);

       // read each byte from input file, encode it, and write to output file
       while (true) {
           int val = in.readBits(BITS_PER_WORD); // read each byte
           if (val == -1) { // EOF
               break;
           }
           String encoding = encodings[val]; // encode each byte
           out.writeBits(encoding.length(), Integer.parseInt(encoding, 2)); // write to output
       }

       // Write the pseudo-EOF character to signal the end of the compressed file
       String code = encodings[PSEUDO_EOF];
       out.writeBits(code.length(), Integer.parseInt(code,2));
       out.close();
   }


   private int[] getCounts(BitInputStream in){ // Get frequency counts of each byte in the input file
       int[] frequencies = new int[ALPH_SIZE];

        // read each byte, count freq
       while (true){
           int value = in.readBits(BITS_PER_WORD);
           if (value == -1){ // EOF
               break;
           }
           frequencies[value]++;
       }
       return frequencies;
   }


   private HuffNode makeTree(int[] counts){ // create huffman tree from frequency counts
      
       PriorityQueue<HuffNode> pq = new PriorityQueue<>(); // pq to build the tree
       for(int i = 0; i < counts.length; i++) {
           if (counts[i] > 0){
               pq.add(new HuffNode(i,counts[i],null,null)); // add leaf nodes
           }
          
       }


       pq.add(new HuffNode(PSEUDO_EOF,1,null,null)); // account for PSEUDO_EOF having a single occurrence



       // Build the tree by combining nodes with the lowest frequencies
       while (pq.size() > 1) {
          HuffNode left = pq.remove();
          HuffNode right = pq.remove();
          // create new HuffNode t with weight from
          // left.weight+right.weight and left, right subtrees
          HuffNode newTree = new HuffNode(0, left.weight + right.weight, left, right);
          pq.add(newTree);
       }
       HuffNode root = pq.remove(); // first element in pq -> make root of tree
       return root;   
   }


   private void makeEncodings(HuffNode root, String path, String[] encodings){ // Create the encodings for each byte using the Huffman tree
        // If it's a leaf node, store the encoding
        if (root.left == null && root.right == null){
           encodings[root.value] = path;
           return;
       }

       // Traverse the left subtree with a "0" added to the path
       makeEncodings(root.left, path + "0", encodings);

       // Traverse the right subtree with a "1" added to the path
       makeEncodings(root.right, path + "1", encodings);
   }


   private void writeTree(HuffNode root, BitOutputStream out){
        // Write the Huffman tree to the output file
       if (root.left == null && root.right == null) { // If it's a leaf node, write a "1" followed by the character
           out.writeBits(1, 1);
           out.writeBits(BITS_PER_WORD + 1, root.value);
       }
       else { // If it's an internal node, write a "0" and recurse for left and right children
           out.writeBits(1, 0);
           writeTree(root.left, out);
           writeTree(root.right, out);
       }


   }


   /**
    * Decompresses a file. Output file must be identical bit-by-bit to the
    * original.
    *
    * @param in
    *            Buffered bit stream of the file to be decompressed.
    * @param out
    *            Buffered bit stream writing to the output file.
    */
   public void decompress(BitInputStream in, BitOutputStream out){


       // remove all code when implementing decompress
       // copies the first file to another file

        // read and validate magic number
       int bits = in.readBits(BITS_PER_INT); 
       if (bits == -1){
           throw new HuffException("Illegal header starts with " + bits);
       }
       if (bits != HUFF_TREE){
           throw new HuffException("Invalid magic number " + bits);
       }

       // read huffman tree from input file
       HuffNode root = readTree(in);
       HuffNode current = root;

       // read compressed bits, decode them using huffman tree
       while (true) {
           int newBits = in.readBits(1);
           if (newBits == -1){
               throw new HuffException("Reading bits has failed");
           }


           else { // traverse tree according to bits read (recall 0 is encoded for left, 1 is encoded for right)
               if (newBits == 0){
                   current = current.left;
               }
               else{
                   current = current.right;
               }

               // reached leaf node
               if (current.right == null && current.left == null){
                   if (current.value == PSEUDO_EOF){ // break at EOF
                       break;
                   }
                   else{
                       out.writeBits(BITS_PER_WORD, current.value); // write decoded byte
                       current = root; // go back to root to repeat process and decode next byte
                   }
               }
              
              
           }
       }
       out.close();


   }


   private HuffNode readTree(BitInputStream in) { // pre-order traversal
       int bit = in.readBits(1); // read next bit from input stream
       if (bit == -1) throw new HuffException("no more bits to read" + bit); // check for end of stream
       if (bit == 0) { // check for internal node
               HuffNode left = readTree(in); // recursively read left subtree
               HuffNode right = readTree(in); // recursively read right subtree
                return new HuffNode(0,0,left,right); // create new internal node w left and right subtrees
       }
       else { // bit == 1 -> leafNode, no recursion needed
           int value = in.readBits(1 + BITS_PER_WORD); // 9 bits of the value stored in the leaf node
           return new HuffNode(value,0,null,null); // create and return new leaf node with the value
       }
 }


}
