# compression_algo
Using Huffman coding to implement decompression and compression algorithms. 

## Part 1: Implementing `HuffProcessor.decompress`

There are four conceptual parts in this function:
1. Read the 32-bit "magic" number as a check on whether the file is Huffman-coded 
2. Read the tree used to decompress, this is the same tree that was used to compress, i.e., was written during compression 
3. Read the bits from the compressed file and use them to traverse root-to-leaf paths, writing leaf values to the output file. Stop when finding `PSEUDO_EOF` 
4. Close the output file

## Part 2: Implementing `HuffProcessor.compress`

There are five conceptual parts in this function:
1. Determine the frequency of every eight-bit character/chunk in the file being compressed 
2. From the frequencies, create the Huffman trie/tree used to create encodings greedily 
3. From the trie/tree, create the encodings for each eight-bit character chunk 
4. Write the magic number and the tree to the beginning/header of the compressed file 
5. Read the file again and write the encoding for each eight-bit chunk, followed by the encoding for PSEUDO_EOF, then close the file being written (not shown).

### Determining Frequencies (private int[] getCounts)

Created an integer array that can store 256 values (use `ALPH_SIZE`). Reading 8-bit characters/chunks, (using `BITS_PER_WORD` rather than 8), and using the read/8-bit value as an index into the array, incrementing the frequency. Conceptually this is a map from 8-bit chunk to an `int` frequency for that chunk, it's easy to use an array for this, mapping the index to the number of times the index occurs, e.g., `counts['a']` is the number of times 'a' occurs in the input file being compressed. -1 indicates there are no more bits to be read in the input stream.

### Making Huffman Trie/Tree (private HuffNode makeTree)

Using a greedy algo and a `PriorityQueue` of `HuffNode` objects to create the trie. Since `HuffNode` implements `Comparable` (using weight), the code removes the minimal-weight nodes when `pq.remove()` is called.

### Making Codings from Trie/Tree (private makeEncodings)

Implemented a recursive helper method, `makeEncodings'. This method populates an array of Strings such that `encodings[val]` is the encoding of the 8-bit chunk val. The recursive helper method has the array of encodings as one parameter, a node that's the root of a subtree as another parameter, and a string that's the path to that node as a string of zeros and ones. If the `HuffNode` parameter is a leaf (recall that a leaf node has no left or right child), an encoding for the value stored in the leaf is added to the array. If the root is not a leaf, we make recursive calls adding "0" to the end of the path when making a recursive call on the left subtree and adding "1" to the end of the path when making a recursive call on the right subtree. Every node in a Huffman tree has two children. 

### Writing the Tree (private void writeTree)

If a node is an internal node, i.e., not a leaf, we write a single bit of zero. Else, if the node is a leaf, we write a single bit of one, followed by _nine bits_ of the value stored in the leaf.  

This is a pre-order traversal: write one bit for the node, then make two recursive calls if the node is an internal node. No recursion is used for leaf nodes. We write 9 bits, or `BITS_PER_WORD + 1`, because there are 257 possible values including `PSEUDO_EOF`.

### Writing Compressed Bits

After writing the tree, we'll read the file being compressed one more time. The ***`BitInputStream` is reset, then read again to compress it***. The first reading was to determine frequencies of every 8-bit chunk. The encoding for each 8-bit chunk read is stored in the array created when encodings were made from the tree. These encodings are stored as strings of zeros and ones, e..g., "010101110101". To convert such a string to a bit-sequence we use `Integer.parseInt` specifying a radix, or base of two. 

We write these bits _after_ writing the bits for every 8-bit chunk. The encoding for `PSEUDO_EOF` is used when decompressing, so we write the encoding bits before the output file is closed.

*Project completed for the Spring 2024 Data Structures class at Duke University.*
