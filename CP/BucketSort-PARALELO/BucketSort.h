#ifndef __BUCKET__
#define __BUCKET__

#define N_BUCKETS 10
#define MAX_RANDOM 1000

struct Bucket {
    int* array;
    size_t n_elem;
    size_t max_elem;
};

void insert(struct Bucket* arr, int elem);
void bucket_sort(size_t size, int* arr);
void print_arr(int* arr, size_t size);
int* Generate_list(size_t size);
void free_bucket(struct Bucket* b);


#endif
