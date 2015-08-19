package com.pipperpublishing.fivekings;

import java.util.ArrayList;

/**
 * Created by Jeffrey on 1/28/2015.
 * 1/28/2015    Creates all the permutations of the integer control structure 1,2,3...,N
 *              using the Johnson-Trotter algorithm (http://en.wikipedia.org/wiki/Steinhaus%E2%80%93Johnson%E2%80%93Trotter_algorithm)
 *              14! (the final round) has 87,178,291,200 combinations so we cant store these
 *              (probably can't generate them all)
 * 1/29/2015    Rewrote Johnson-Trotter as loop and computes and returns next permutation (so no storage)
 * 3/4/2015     Removed deprecated
 */
    class Permuter {
        private int[] perms;
        private int[] indexPerms;
        private int[] directions;
        private int[] iSwap;
        private int N; //permute 0..N-1
        private int movingPerm=N;

        static int FORWARD=+1;
        static int BACKWARD=-1;


        Permuter(int N) {
            this.N = N;
            perms =  new int[N];     // permutations
            indexPerms = new int[N];     // index to where each permutation value 0..N-1 is
            directions = new int[N];     // direction = forward(+1) or backward (-1)
            iSwap = new int[N]; //number of swaps we make for each integer at each level
            for (int i = 0; i < N; i++) {
                directions[i] = BACKWARD;
                perms[i]  = i;
                indexPerms[i] = i;
                iSwap[i] = i;
            }
            movingPerm = N;
        }

        int[] getNext() {
            //each call returns the next permutation
            do{
                if (movingPerm == N) {
                    movingPerm--;
                    return perms;
                } else if (iSwap[movingPerm] > 0) {
                    //swap
                    int swapPerm = perms[indexPerms[movingPerm] + directions[movingPerm]];
                    perms[indexPerms[movingPerm]] = swapPerm;
                    perms[indexPerms[movingPerm] + directions[movingPerm]] = movingPerm;
                    indexPerms[swapPerm] = indexPerms[movingPerm];
                    indexPerms[movingPerm] = indexPerms[movingPerm] + directions[movingPerm];
                    iSwap[movingPerm]--;
                    movingPerm=N;
                } else {
                    iSwap[movingPerm] = movingPerm;
                    directions[movingPerm] = -directions[movingPerm];
                    movingPerm--;
                }
            } while (movingPerm > 0);
            return null;
        }

    private void JohnsonTrotterGenerate(int N, int[] perms, int[] inversePerms, int[] directions) {
        // base case - print out permutation
        ArrayList<int[]> permutations=null;
        if (N >= perms.length) permutations.add(perms.clone());
        else {
            JohnsonTrotterGenerate(N + 1, perms, inversePerms, directions);
            for (int i = 0; i < N; i++) {
                // swap
                int z = perms[inversePerms[N] + directions[N]];
                perms[inversePerms[N]] = z;
                perms[inversePerms[N] + directions[N]] = N;
                inversePerms[z] = inversePerms[N];
                inversePerms[N] = inversePerms[N] + directions[N];

                JohnsonTrotterGenerate(N + 1, perms, inversePerms, directions);
            }
            directions[N] = -directions[N];
        }
    }

    private void HeapAlgorithmGenerate(int[] v, int N) {
        int exchangeWith;
        //instead of this, provide some way to return this value and then restart from this point
        ArrayList<int[]> permutations=null;
        if (N==1) permutations.add(v.clone());
        else {
            for (int i = 0; i < N; i++) {
                HeapAlgorithmGenerate(v, N - 1);
                //swap the last element with either the first or this element
                exchangeWith = (N % 2 == 1 ? 0 : i);
                swap(v, exchangeWith, N - 1);
            }
        }
    }

    private static void swap(int[] v, int i, int j) {
        int t = v[i];
        v[i] = v[j];
        v[j] = t;
    }

}
