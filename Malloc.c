/*
 * mm.c
 *
 * NOTE TO STUDENTS: Replace this header comment with your own header
 * comment that gives a high level description of your solution.
 */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "mm.h"
#include "memlib.h"

/* If you want debugging output, use the following macro.  When you hand
 * in, remove the #define DEBUG line. */
#define DEBUG
#ifdef DEBUG
# define dbg_printf(...) printf(__VA_ARGS__)
#else
# define dbg_printf(...)
#endif


/* do not change the following! */
#ifdef DRIVER
/* create aliases for driver tests */
#define malloc mm_malloc
#define free mm_free
#define realloc mm_realloc
#define calloc mm_calloc
#endif /* def DRIVER */

/* single word (4) or double word (8) alignment */
#define ALIGNMENT 8

/* rounds up to the nearest multiple of ALIGNMENT */
#define ALIGN(p) (((size_t)(p) + (ALIGNMENT-1)) & ~0x7)

#define WSIZE       4       /* Word and header/footer size (bytes) */ 
#define DSIZE       8       /* Doubleword size (bytes) */
#define CHUNKSIZE  (1<<9)  /* Extend heap by this amount (bytes) */   

#define MAX(x, y) ((x) > (y)? (x) : (y))  

/* Pack a size and allocated bit into a word */
#define PACK(size, alloc)  ((size) | (alloc)) //line:vm:mm:pack

/* Read and write a word at address p */
#define GET(p)       (*(unsigned int *)(p))    
#define GET_VAL(p)       (*(unsigned long *)(p))            
#define PUT(p, val)  (*(unsigned int *)(p) = (val))    
#define PUT_VAL(p, val)  (*(unsigned long *)(p) = (unsigned long)(val)) 

/* Read the size and allocated fields from address p */
#define GET_SIZE(p)  (GET(p) & ~0x7)                   
#define GET_ALLOC(p) (GET(p) & 0x1)                    


//#define RESET_PREV_BIT(p) (PUT(HDRP(p),PACK(GET_SIZE(HDRP(p)),GET_ALLOC(HDRP(p))&0x)))

/* Given block ptr bp, compute address of its header and footer */
#define HDRP(bp)       ((char *)(bp) - WSIZE)                      
#define FTRP(bp)       ((char *)(bp) + GET_SIZE(HDRP(bp)) - DSIZE) 

/* Given block ptr bp, compute address of next and previous blocks */
#define NEXT_BLKP(bp)  ((char *)(bp) + GET_SIZE(((char *)(bp) - WSIZE))) 
#define PREV_BLKP(bp)  ((char *)(bp) - GET_SIZE(((char *)(bp) - DSIZE))) 

#define GET_NEXT(bp)  (*((unsigned long *)(bp)))
#define GET_PREV(bp)  (*(((unsigned long *)(bp)) + 1))
#define NEXT(bp)       ((unsigned long *)bp+1)
#define PREV(bp)		((unsigned long*)bp-1)
/* $end mallocmacros */

#ifdef NEXT_FIT
    static char *rover;           /* Next fit rover */
#endif

static char *heap_listp = 0;  /* Pointer to first block */
static unsigned long *free_pointer;  /* Pointer to first free block */
static unsigned long *free_pointer1;

/* Function prototypes for internal helper routines */
static void *extend_heap(size_t words);
static void place(void *bp, size_t asize);
static void *find_fit(size_t asize);
static void *coalesce(void *bp);
static void printblock(void *bp); 
static void insert_freeblock(void * bp);
static void remove_freeblock(void *bp);
static void printheap();
static unsigned long* findlist(size_t asize);


//static void checkheap(int verbose);
static void checkblock(void *bp);
static int v = 0;

/*
 * Initialize: return -1 on error, 0 on success.
 */
int mm_init(void) {
	char *bp;
	int i;
	
    /* Creating an initial empty heap */
    if ((heap_listp = mem_sbrk(22*WSIZE)) == (void *)-1) {
        return -1;
    }
    if(v)
        printf("Inside init\n");
    PUT(heap_listp, 0);              /* Alignment padding */
	char* temp=heap_listp+WSIZE;
	free_pointer1=(unsigned long *)temp;
	if(v)
	printf("Value of free pointer; %p\n",(void *)(free_pointer));
	for(i=0;i<9;i++)
	{	
		PUT_VAL(temp,0);
		if(v)
		printf("value of temp: %p\n",(void * )(temp));
		temp+=DSIZE;
	}
	
	
	
    PUT(temp , PACK(DSIZE, 1)); /* Prologue header */
	if(v)
	printf("value of temp for p head: %p\n",(void * )(temp));	
    PUT(temp + (1*WSIZE), PACK(DSIZE, 1)); /* Prologue footer */
	if(v)
	printf("value of temp for p foot %p\n",(void * )(temp+1*WSIZE));
    PUT(temp + (2*WSIZE), PACK(0, 1));     /* Epilogue header */
	if(v)
	printf("value of temp for ep header: %p\n",(void * )(temp+2*WSIZE));
    heap_listp += (20*WSIZE);
	if(v)	
	printf("value of heap_listp:%p\n",(void *)heap_listp);
    
	 
    /* Extend the empty heap with a free block of CHUNKSIZE bytes */
    if ((bp = extend_heap(CHUNKSIZE/WSIZE)) == NULL){
        return -1;
    }
	
	
	//PUT_VAL(free_pointer1+4,(unsigned long *)bp);
	//printheap();
    
	PUT_VAL(bp,0);
	PUT_VAL(bp + DSIZE,0);
	if(v)
    printheap();
	//exit(0);
    return 0;
}

/*
 * malloc
 */
void *malloc (size_t size) {
    size_t asize;      /* Adjusted block size */
    size_t extendsize; /* Amount to extend heap if no fit */
    char *bp; 
    
    if (heap_listp == 0){
        mm_init();
    }
    
    /* Ignoring spurious requests */
    if (size == 0){
        return NULL;
    }

    /* Adjust block size to include overhead and alignment reqs. */
    if (size <= 2 * DSIZE){    
        asize = 3 * DSIZE;
    }
    else{
        asize = DSIZE * ((size + (DSIZE) + (DSIZE-1)) / DSIZE);
    }

    
    /* Search the free list for a fit */
    if ((bp = find_fit(asize)) != NULL){
        if(v)
        printf("bp = %p\n",(void *)bp);
        place(bp, asize);
        return bp;
    }

    /* No fit found. Get more memory and place the block */
    extendsize = MAX(asize,CHUNKSIZE);
    
    if ((bp = extend_heap(extendsize/WSIZE)) == NULL){
        return NULL;
    }
    //if(v)
	//mm_checkheap(1);
	if(v)
	printheap();
    place(bp, asize);
    return bp;
}

/*
 * free
 */
void free (void *ptr) {
	
    if(v)
	printf("inside free:\n");
    if(ptr == 0){
        return;
    }
    
    size_t size = GET_SIZE(HDRP(ptr));
    
    if (heap_listp == 0){
        mm_init();
    }

    PUT(HDRP(ptr), PACK(size, 0));
    PUT(FTRP(ptr), PACK(size, 0));
    
    PUT_VAL((unsigned long*)ptr,0);
    PUT_VAL((unsigned long*)ptr +1,0);
   
    coalesce(ptr);
	//if(v)
	//mm_checkheap(1);
}

/*
 * coalesce - Boundary tag coalescing. Return ptr to coalesced block
 */
static void *coalesce(void *bp){

        
    size_t prev_alloc = GET_ALLOC(FTRP(PREV_BLKP(bp)));
    size_t next_alloc = GET_ALLOC(HDRP(NEXT_BLKP(bp)));
    size_t size = GET_SIZE(HDRP(bp));

    if (prev_alloc && next_alloc) {   /* Case 1 */
		if(v)
		printf("case:1:\n");
		insert_freeblock(bp);
		return bp;
    }

    else if (prev_alloc && !next_alloc) {      /* Case 2 */
		if(v)
		printf("case:2:\n");
		remove_freeblock(NEXT_BLKP(bp));
		size += GET_SIZE(HDRP(NEXT_BLKP(bp)));
		
		PUT(HDRP(bp), PACK(size, 0));
		PUT(FTRP(bp), PACK(size,0));
		insert_freeblock(bp);
		return bp;
		
    }

    else if (!prev_alloc && next_alloc) {      /* Case 3 */
		if(v)
		printf("case:3:\n");
		remove_freeblock(PREV_BLKP(bp));
		size += GET_SIZE(HDRP(PREV_BLKP(bp)));
		PUT(FTRP(bp), PACK(size, 0));
		PUT(HDRP(PREV_BLKP(bp)), PACK(size, 0));
		bp = PREV_BLKP(bp);
		insert_freeblock(bp);
		return bp;
    }

    else {                                     /* Case 4 */
		if(v)
		printf("case:4:\n");
		remove_freeblock(NEXT_BLKP(bp));
		remove_freeblock(PREV_BLKP(bp));
		size += GET_SIZE(HDRP(PREV_BLKP(bp))) + 
	    GET_SIZE(FTRP(NEXT_BLKP(bp)));
		
		
		
		PUT(HDRP(PREV_BLKP(bp)), PACK(size, 0));
		PUT(FTRP(NEXT_BLKP(bp)), PACK(size, 0));
		bp = PREV_BLKP(bp);
		
		insert_freeblock(bp);
		return bp;
    }
   
	if(v)
	printheap();
    
    return bp;
}

/*
 * realloc - you may want to look at mm-naive.c
 */
void *realloc(void *oldptr, size_t size) {

    if(v)
        printf("Realloc !\n");
    size_t oldsize;
    void *newptr;

    /* If size == 0 then this is just free, and we return NULL. */
    if(size == 0) {
        free(oldptr);
        return 0;
    }

    /* If oldptr is NULL, then this is just malloc. */
    if(oldptr == NULL) {
        return malloc(size);
    }

        newptr = malloc(size);

    /* If realloc() fails the original block is left untouched  */
    if(!newptr) {
        return 0;
    }

    /* Copy the old data. */
    oldsize = GET_SIZE(HDRP(oldptr));
    if(size < oldsize){ 
        oldsize = size;
    }
    memcpy(newptr, oldptr, oldsize);

    /* Free the old block. */
    free(oldptr);
	//if(v)
	//mm_checkheap(1);
    return newptr;
}

/*
 * calloc - you may want to look at mm-naive.c
 * This function is not tested by mdriver, but it is
 * needed to run the traces.
 */
void *calloc (size_t nmemb, size_t size) {
    if(nmemb == 0){
        return NULL;
    } else if(size == 0){
        return NULL;
    }
    return NULL;
}


/*
 * Return whether the pointer is in the heap.
 * May be useful for debugging.
 */
static int in_heap(const void *p) {
    return p <= mem_heap_hi() && p >= mem_heap_lo();
}

/*
 * Return whether the pointer is aligned.
 * May be useful for debugging.
 */
static int aligned(const void *p) {
    return (size_t)ALIGN(p) == (size_t)p;
}

/*
 * mm_checkheap
 */
void mm_checkheap(int verbose){
    char *bp = heap_listp;
    int flag = -1;
    unsigned int prev_alloc_bit;
    unsigned long * temp = free_pointer;
    unsigned long * next_ptr;
    unsigned int count_free = 0, count_heap = 0;
    //unsigned int count_heap = 0;

    if (verbose){
        if(v)
        printf("Heap (%p):\n", heap_listp);
    }

    if ((GET_SIZE(HDRP(heap_listp)) != DSIZE) || !GET_ALLOC(HDRP(heap_listp))){
        printf("Bad prologue header\n");
		exit(0);
    }
    checkblock(heap_listp);

    for (bp = heap_listp; GET_SIZE(HDRP(bp)) > 0; bp = NEXT_BLKP(bp)){
        if (verbose){
            printblock(bp);
        }
        aligned(bp);
        if(GET_ALLOC(HDRP(bp)) != 1){
            count_heap++;
        }
    }
    
    for (bp = heap_listp; GET_SIZE(HDRP(bp)) > 0; bp = NEXT_BLKP(bp)){
        checkblock(bp);
        
        if(flag != -1){
            flag = 1;
            
            if (prev_alloc_bit == GET_ALLOC(HDRP(bp)) 
                && GET_ALLOC(HDRP(bp)) == 0){
                printf("Error: Consecutive free blocks should not be there !\n");
            }
        }
        prev_alloc_bit = GET_ALLOC(HDRP(bp));
    }

    if (verbose){
        printblock(bp);
    }
    if ((GET_SIZE(HDRP(bp)) != 0) || !(GET_ALLOC(HDRP(bp)))){
        printf("Bad epilogue header\n");
    }
    
    for(temp = free_pointer; temp != 0; temp = ((unsigned long *)GET_NEXT(temp))){
        count_free++;
        if((*temp) != 0){
            if(v)
            printf("Entered for \n");
            next_ptr = (unsigned long *)(*temp);
            if((*(next_ptr+1)) != (unsigned long)(temp+1)){
                printf("Pointer mismatch\n");
                exit(0);
            }
            if(!in_heap(temp)){
                printf("Free list pointer outside the range !!\n");
            }
        } 
    }
    
	
	
    if(count_free != count_heap){
    if(v)
        printf("Counts should be the same !!\n");
    }
}

/* 
 * extend_heap - Extend heap with free block and return its block pointer
 */
static void *extend_heap(size_t words){
    char *bp;
    size_t size;
	if(v)
	printf("Inside Extend heap\n");

    /* Allocate an even number of words to maintain alignment */
    size = (words % 2) ? (words+1) * WSIZE : words * WSIZE;
    if ((long)(bp = mem_sbrk(size)) == -1){
        return NULL;
    }

    /* Initialize free block header/footer and the epilogue header */
    PUT(HDRP(bp), PACK(size, 0));         /* Free block header */
    PUT(FTRP(bp), PACK(size, 0));         /* Free block footer */
	
    PUT(HDRP(NEXT_BLKP(bp)), PACK(0, 1)); /* New epilogue header */
	PUT_VAL(bp,0);
	PUT_VAL(NEXT(bp),0);
	
    /* Coalesce if the previous block was free */
    return coalesce(bp);
}

/* 
 * place - Place block of asize bytes at start of free block bp 
 *         and split if remainder would be at least minimum block size
 */
static void place(void *bp, size_t asize){
	   if(v)
	   printf("Inside place;");
	size_t csize = GET_SIZE(HDRP(bp));

    if ((csize - asize) >= (3*DSIZE)){ 
		remove_freeblock(bp);
		PUT(HDRP(bp), PACK(asize, 1));
        PUT(FTRP(bp), PACK(asize, 1));
        
		bp = NEXT_BLKP(bp);
		
        PUT(HDRP(bp), PACK(csize-asize, 0));
        PUT(FTRP(bp), PACK(csize-asize, 0));
		PUT_VAL(bp,0);
		PUT_VAL(NEXT(bp),0);
		insert_freeblock(bp);
		
    } else {
    
        remove_freeblock(bp);
        PUT(HDRP(bp), PACK(csize, 1));
        PUT(FTRP(bp), PACK(csize, 1));
	
    }
    if(v)
	printheap();
}

/* 
 * find_fit - Find a fit for a block with asize bytes 
 */
static void *find_fit(size_t asize){

    unsigned long *bp;
	int flag=1;
	unsigned long* ptr;
	if(v)
	printf("Allocate size %d\n",(int)asize);
	
	ptr=findlist(asize);
	if(v)
	printf("findlist value: %p\n",(void *)ptr);
	
    //bp = (unsigned long *)GET_VAL(findlist(asize));
	
	
	
	for(bp=(unsigned long *)GET_VAL(ptr);ptr<=(free_pointer1+8);ptr++){
		bp=(unsigned long *)GET_VAL(ptr);
		if(v){
		printf("Value of bp obtained inside find fit %p,\n",(void * )bp);
		printf("Address of free list %p \n",(void *)ptr);
		}
		for (; bp != 0; bp = ((unsigned long *)GET_NEXT(bp))){
        if(v)
		printf("Finding .. \n");
		if (asize <= GET_SIZE(HDRP(bp))){
        if(v)
			printf("Found!\n");
			flag=0;
			return bp;
			}
		}
	}
	
	
    if(v)
    printf("Not found !\n");
    return NULL; /* No fit */
    //#endif
}

static void printblock(void *bp){
    size_t hsize, halloc, fsize, falloc;

    //checkheap(0);
    hsize = GET_SIZE(HDRP(bp));
    halloc = GET_ALLOC(HDRP(bp));  
    fsize = GET_SIZE(FTRP(bp));
    falloc = GET_ALLOC(FTRP(bp));  

    if (hsize == 0) {
    if(v)
	printf("%p: EOL\n", bp);
	return;
    }

    if(v)
    printf("%p: header: [%d:%c] pointers: [%p:%p] footer: [%d:%c]\n", bp, 
    (int)hsize, (halloc ? 'a' : 'f'), (unsigned long*)*((unsigned long*)bp), (unsigned long*)(*((unsigned long*)bp+1)),
    (int)fsize, (falloc ? 'a' : 'f'));
}

static void checkblock(void *bp) {
    if ((size_t)bp % 8){
        printf("Error: %p is not doubleword aligned\n", bp);
    }
    if(GET_ALLOC(HDRP(bp))==0){
		if (GET(HDRP(bp)) != GET(FTRP(bp))){
			printf("%d %d \n", (int) GET(HDRP(bp)), (int)GET(FTRP(bp)));
			printf("Error: header does not match footer\n");
		}
	}
    if( bp != heap_listp && GET_SIZE(HDRP(bp)) < 3*DSIZE){
		printf("%d\n",GET_SIZE(HDRP(bp)));
        printf("Error: size cannot be so less than 8 bytes\n");
    }
}

void checkheap(int verbose){
    char *bp = heap_listp;

    if (verbose)
    if(v)
	printf("Heap (%p):\n", heap_listp);

    if ((GET_SIZE(HDRP(heap_listp)) != DSIZE) || !GET_ALLOC(HDRP(heap_listp)))
	printf("Bad prologue header\n");
    checkblock(heap_listp);

    for (bp = heap_listp; GET_SIZE(HDRP(bp)) > 0; bp = NEXT_BLKP(bp)) {
	if (verbose) 
	    printblock(bp);
		
		
	checkblock(bp);
    }

    if (verbose)
	printblock(bp);
    if ((GET_SIZE(HDRP(bp)) != 0) || !(GET_ALLOC(HDRP(bp))))
	printf("Bad epilogue header\n");
}
void insert_freeblock(void *bp){

	if(v)
	printf("Inside insert block\n");
	//printf("value of bp: %p",bp);
	unsigned long * temp;
	unsigned long * first_free;
	int size = GET_SIZE(HDRP(bp));
	if(v)
	printf(" Size of block to be inserted %d\n",size);
	temp=findlist(size);
	if(v)
	printf("value of free list: %p\n",temp);
	first_free=(unsigned long *)GET_VAL(temp);
	
	if(first_free==0)
	  { //PUT_VAL(bp,0);
		if(v)
		printf("If");
	    PUT_VAL(NEXT(bp),0);
		PUT_VAL(bp,0);
	 }else{
		if(v)
		printf("else \n");
		PUT_VAL(bp,first_free);
		PUT_VAL(NEXT(first_free),NEXT(bp));
		PUT_VAL(NEXT(bp),0);
		
		}
	 //first_free = bp;
	 PUT_VAL(temp,bp);
	 if(v)
	 printheap();
	 }
void remove_freeblock(void *bp){
	
	if(v)
	printf("Inside remove block\n");
	unsigned long * next_ptr;
	unsigned long * prev_ptr;
	next_ptr = (unsigned long *)GET_VAL(bp);
	prev_ptr = (unsigned long *)GET_VAL(NEXT(bp));
	if(v){
	printf("Value of bp %p\n",bp);
	printf("Value of next_ptr %p\n",(void *) next_ptr);
	printf("Value of prev_ptr %p\n",(void *) prev_ptr);
	}
	unsigned long * temp;
	unsigned long * first_free;
	int size = GET_SIZE(HDRP(bp));
	temp=findlist(size);
	if(v)
	printf("value of free list: %p\n",temp);
	first_free=(unsigned long *)GET_VAL(temp);
	
	if(next_ptr != 0 && prev_ptr != 0){
		if(v)
		printf("Case 1 in remove\n");
		
		PUT_VAL(next_ptr + 1,prev_ptr);
		PUT_VAL(prev_ptr-1,next_ptr);
	}else if(prev_ptr != 0){
			if(v)
		printf("Case 2 in remove\n");
		PUT_VAL(prev_ptr-1,0);
	}else if(next_ptr!= 0){
			if(v)
		printf("Case 3 in remove\n");
		PUT_VAL(next_ptr+1,0);
		PUT_VAL(temp,next_ptr);
		//first_free=next_ptr;
	}else{
			if(v)
		printf("Case 4 in remove\n");
		if(v)
		printf("Value of first-free and bp %p %p",(void *)first_free,(void *)bp);
		if(first_free==bp)
		{
		PUT_VAL(temp,0);
		//first_free=0;
		}
	}
	if(v)
	printheap();
}






unsigned long * findlist(size_t asize)
{    //unsigned long * temp=0;
	int i;
	if(v)
	printf("Size inside findlist %d\n",(int)asize);
	for(i=5;i<=13;i++)
	{
		if(asize <=((unsigned int)1<<i)){
		if(v)
		printf("Value returned %p\n",(free_pointer1+(i-5)));
		return (free_pointer1+(i-5));
		}
	
	}
	if(v)
	printf("Value returned %p\n",(free_pointer1+8));
	return (free_pointer1+8);
}
	
void printheap()

{	int i; 
	char * bp;
	printf("\nInside printheap\n");
	printf("value of free pointer %p\n",(void *)free_pointer1);
	for (i=0;i<9;i++){
	
	printf("Address: %p Value %lx \n",(void *)(free_pointer1+i),GET_VAL(free_pointer1+i));

	}
	for (bp = heap_listp; GET_SIZE(HDRP(bp)) > 0; bp = NEXT_BLKP(bp)) {
	if (v) 
	    printblock(bp);

    }

}	
		
		
		

