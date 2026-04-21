static ee_t real_code(const w_t *a)
{
    conf_t b;

    bool c = (a != NULL);
    if (c)
    {
        memset(b.d.e, UINT8_MAX, sizeof(b.d.e));
        if (a->k != NULL)
        {
            strlcpy((char *)b.d.e, a->k, sizeof(b.d.e));
        }
        memset(b.d.f, 0, sizeof(b.d.f));
        if (a->l != NULL && strlen(a->l) > 0)
        {
            strlcpy((char *)b.d.f, a->l, sizeof(b.d.f));
            b.d.g.h = CONST1;
        }
        else
        {
            b.d.g.h = CONST2;
        }
    }
    else if (!lc(b.d.e, sizeof(b.d.e) + sizeof(b.d.f)))
    {
        return ERR;
    }
    return sc(CONST3, &b);
} 
