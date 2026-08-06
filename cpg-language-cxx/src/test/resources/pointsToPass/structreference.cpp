/* Part of Juliet Test Suite
see https://github.com/arichardson/juliet-test-suite-c/blob/master/testcases/CWE416_Use_After_Free/CWE416_Use_After_Free__malloc_free_struct_07.c
*/

void printStructLine (const twoIntsStruct * structTwoIntsStruct)
{
    printf("%d -- %d\n", structTwoIntsStruct->intOne, structTwoIntsStruct->intTwo);
}

void CWE416_Use_After_Free__malloc_free_struct_07_bad()
{
    twoIntsStruct * data;
    /* Initialize data */
    data = NULL;
    if(staticFive==5)
    {
        data = (twoIntsStruct *)malloc(100*sizeof(twoIntsStruct));
        if (data == NULL) {exit(-1);}
        {
            size_t i;
            for(i = 0; i < 100; i++)
            {
                data[i].intOne = 1;
                data[i].intTwo = 2;
            }
        }
        /* POTENTIAL FLAW: Free data in the source - the bad sink attempts to use data */
        free(data);
    }
    if(staticFive==5)
    {
        /* POTENTIAL FLAW: Use of data that may have been freed */
        printStructLine(&data[0]);
        /* POTENTIAL INCIDENTAL - Possible memory leak here if data was not freed */
    }
}
