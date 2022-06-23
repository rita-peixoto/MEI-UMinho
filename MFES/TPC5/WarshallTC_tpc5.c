#define MAXVERTICES 10
typedef int Graph[MAXVERTICES][MAXVERTICES];

 /*@ requires 0 < n <= MAXVERTICES  &&
  @             \valid_read(A+(0..(n*n-1))) &&
  @             \valid(R+(0..(n*n-1))) &&
  @             \separated(A+(0..n*n-1), R+(0..n*n-1));
  @ assigns R[0..n-1][0..n-1];
  @ ensures \forall integer k, l;
  @             0 <= k < n && 0 <= l < n ==> A[k][l] == \at(A[k][l],Old);
  @*/ 
void WarshallTC (Graph A, Graph R, int n) {

    int i, j, k;

    /*@
      @ loop invariant 0 <= i <= n && 
      @                 \forall integer a, b; 0 <= a < i && 0 <= b < n ==> A[a][b] == R[a][b];
      @ loop assigns i, j, R[0..n-1][0..n-1];
      @ loop variant n-i;
      @*/
    for (i=0 ; i<n ; i++)

        /*@
          @ loop invariant 0 <= j <= n;
          @ loop invariant \forall integer b; 0 <= b < j ==> A[i][b] == R[i][b];
          @ loop assigns j, R[i][0..n-1];
          @ loop variant n-j;
          @*/
        for (j=0 ; j<n ; j++)
            R[i][j] = A[i][j];

    /*@ assert \forall integer k, l;
      @    0 <= k < n && 0 <= l < n ==> A[k][l] == R[k][l];
      @*/
    
    /*@
      @ loop invariant 0 <= k <= n;
      @ loop assigns R[0..n-1][0..n-1],k,j,i;
      @ loop variant n-k;
      @*/
    for (k=0 ; k<n; k++)
        /*@
          @ loop invariant 0 <= i <= n ;
          @ loop assigns R[0..n-1][0..n-1],i,j;         
          @ loop variant n-i;
          @*/
        for (i=0 ; i<n ; i++)
            /*@
              @ loop invariant 0 <= j <= n ;
              @ loop assigns R[i][0..n-1], j;
              @ loop variant n-j;
              @*/
            for (j=0 ; j<n ; j++)
                if (R[i][k] && R[k][j])
                    R[i][j] = 1;
}