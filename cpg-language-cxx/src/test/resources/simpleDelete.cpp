static void simpleDeleteTest()
{
    int64_t * x = NULL;
    for(int i = 0; i < 1; i++)
    {
        x = new int64_t;
        *x = 5LL;
        delete x; //free(x)
    }
    use(*x);
}
