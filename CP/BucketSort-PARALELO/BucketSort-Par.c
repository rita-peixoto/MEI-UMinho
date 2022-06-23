#include <unistd.h>
#include <omp.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "BucketSort.h"

struct Bucket* make(size_t max){
    struct Bucket* block_arr = malloc(sizeof(struct Bucket));
    block_arr->array = malloc(sizeof(int) * max);
    block_arr->n_elem = 0;
    block_arr->max_elem = max;

    return block_arr;
}

void free_bucket(struct Bucket* b){
    free(b->array);
    free(b);
}

// adiciona o elemento no bucket respetivo 
void insert(struct Bucket* arr, int elem){ 

   // verificar se o bucket ainda esta completo
   if(arr->n_elem >= arr->max_elem){

      // caso nao tiver espaco entao aloca se mais espaco ao array
      arr->array = realloc(arr->array, sizeof(int) * arr->max_elem * 2);
      arr->max_elem *= 2; // n max de elementos e aumentado
   }

   arr->array[arr->n_elem] = elem; // adiciona elementos
   arr->n_elem++; // aumenta o numero de elementos
}


int cmpfunc (const void * a, const void * b) {
    return ( *(int*)a - *(int*)b );
}

void bucket_sort(size_t size, int* arr){

    if (!size){
        fprintf(stderr, "Can't calculate max of empty array");
        _exit(1);
    }

    int max = arr[0];
    int min = arr[0];
    for(size_t i = 1; i < size; i++) { //vectorized
        if(arr[i] > max) max = arr[i];
        if(arr[i] < min) min = arr[i];
    }

    struct Bucket* buckets[N_BUCKETS];
    int threads =  N_BUCKETS;

    //PASSO 1
    #pragma omp parallel num_threads(threads)
    #pragma omp for
    for (size_t i = 0; i < N_BUCKETS; i++)
        buckets[i] = make(size);
    
    // uma thread para cada bucket
    #pragma omp parallel num_threads(threads)
    #pragma omp for
    for (size_t i = 0; i < N_BUCKETS; i++) {

        for (size_t j = 0; j < size; j++){

            //PASSO 2
            size_t n_bucket = (arr[j] + abs(min)) * N_BUCKETS / (abs(max + abs(min)));
            n_bucket = n_bucket >= N_BUCKETS ? N_BUCKETS - 1 : n_bucket;

            if(n_bucket == i) {

                insert(buckets[i], arr[j]);
            }  
        }

        //PASSO 3
        qsort(buckets[i]->array, buckets[i]->n_elem, sizeof(int), cmpfunc);    
    }

    size_t i = 0, j;

    //PASSO 4
    for(j = 0; j < N_BUCKETS; j++) {

        memcpy(arr + i, buckets[j]->array, buckets[j]->n_elem * sizeof(int));
        i += buckets[j]->n_elem;
        free_bucket(buckets[j]);
    }
}

// print do array
void print_arr(int* arr, size_t size){

   for(size_t i = 0; i < size; i++) {
      arr[i] = arr[i];
      if(i == size-1){
         printf("%d\n", arr[i]);
      } else {
         printf("%d, ", arr[i]);
      }
   }
}
//geracao de um conjunto de elementos para o array  
int* Generate_list(size_t size) {

   int *a = malloc(size*sizeof(int));

   for (int i = 0; i < size; i++)
      a[i] = rand() % MAX_RANDOM;

   return a;
} 