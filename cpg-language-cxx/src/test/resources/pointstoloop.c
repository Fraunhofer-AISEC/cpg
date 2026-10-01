#define GNUNET_memcpy(dst, src, n) \
        do                               \
        {                                \
          if (0 != n)                    \
          {                              \
            (void) memcpy (dst, src, n); \
          }                              \
        } while (0)

#define GNUNET_realloc(ptr, size) \
        GNUNET_xrealloc_ (ptr, size, __FILE__, __LINE__)

#define GNUNET_MIN(a, b) (((a) < (b)) ? (a) : (b))

#if __GNUC__ >= 6 || __clang_major__ >= 6
#define GNUNET_assert(cond)                                     \
        do                                                            \
        {                                                             \
          _Pragma("GCC diagnostic push")                                 \
          _Pragma("GCC diagnostic ignored \"-Wtautological-compare\"")   \
          if (! (cond))                                               \
          {                                                           \
          GNUNET_log (GNUNET_ERROR_TYPE_ERROR,                      \
          dgettext ("gnunet", "Assertion failed at %s:%d. Aborting.\n"), \
          __FILE__,                                     \
          __LINE__);                                    \
          GNUNET_abort_ ();                                         \
          }                                                           \
          _Pragma("GCC diagnostic pop")                                  \
          } while (0)
#else
/* older GCC/clangs do not support -Wtautological-compare */
#define GNUNET_assert(cond)                                     \
        do                                                            \
        {                                                             \
          if (! (cond))                                               \
          {                                                           \
            GNUNET_log (GNUNET_ERROR_TYPE_ERROR,                      \
                        dgettext ("gnunet", \
                                  "Assertion failed at %s:%d. Aborting.\n"), \
                        __FILE__,                                     \
                        __LINE__);                                    \
            GNUNET_abort_ ();                                         \
          }                                                           \
        } while (0)
#endif


#if ! defined(GNUNET_CULL_LOGGING)
#define GNUNET_log_from(kind, comp, ...)                                  \
        do                                                                      \
        {                                                                       \
          static int log_call_enabled = GNUNET_LOG_CALL_STATUS;                 \
          if ((GNUNET_EXTRA_LOGGING > 0) ||                                     \
              ((GNUNET_ERROR_TYPE_DEBUG & (kind)) == 0))                        \
          {                                                                     \
            if (GN_UNLIKELY (log_call_enabled == -1))                           \
            log_call_enabled =                                                \
              GNUNET_get_log_call_status ((kind) & (~GNUNET_ERROR_TYPE_BULK), \
                                          (comp),                             \
                                          __FILE__,                           \
                                          __func__,                       \
                                          __LINE__);                          \
            if (GN_UNLIKELY (GNUNET_get_log_skip () > 0))                       \
            {                                                                   \
              GNUNET_log_skip (-1, GNUNET_NO);                                  \
            }                                                                   \
            else                                                                \
            {                                                                   \
              if (GN_UNLIKELY (log_call_enabled))                               \
              GNUNET_log_from_nocheck ((kind), comp, __VA_ARGS__);            \
            }                                                                   \
          }                                                                     \
        } while (0)

#define GNUNET_log(kind, ...)                                             \
        do                                                                      \
        {                                                                       \
          static int log_call_enabled = GNUNET_LOG_CALL_STATUS;                 \
          if ((GNUNET_EXTRA_LOGGING > 0) ||                                     \
              ((GNUNET_ERROR_TYPE_DEBUG & (kind)) == 0))                        \
          {                                                                     \
            if (GN_UNLIKELY (log_call_enabled == -1))                           \
            log_call_enabled =                                                \
              GNUNET_get_log_call_status ((kind) & (~GNUNET_ERROR_TYPE_BULK), \
                                          NULL,                               \
                                          __FILE__,                           \
                                          __func__,                       \
                                          __LINE__);                          \
            if (GN_UNLIKELY (GNUNET_get_log_skip () > 0))                       \
            {                                                                   \
              GNUNET_log_skip (-1, GNUNET_NO);                                  \
            }                                                                   \
            else                                                                \
            {                                                                   \
              if (GN_UNLIKELY (log_call_enabled))                               \
              GNUNET_log_nocheck ((kind), __VA_ARGS__);                       \
            }                                                                   \
          }                                                                     \
        } while (0)
#else
#define GNUNET_log(...)
#define GNUNET_log_from(...)
#endif

#define LOG(kind, ...) GNUNET_log_from (kind, "dht-clients", __VA_ARGS__)


enum GNUNET_GenericReturnValue
GNUNET_MST_from_buffer (struct GNUNET_MessageStreamTokenizer *mst,
                        const char *buf,
                        size_t size,
                        int purge,
                        int one_shot)
{
  const struct GNUNET_MessageHeader *hdr;
  size_t delta;
  uint16_t want;
  char *ibuf;
  int ret;
  int cbret;

  GNUNET_assert (mst->off <= mst->pos);
  GNUNET_assert (mst->pos <= mst->curr_buf);
  LOG (GNUNET_ERROR_TYPE_DEBUG,
       "MST receives %u bytes with %u (%u/%u) bytes already in private buffer\n",
       (unsigned int) size,
       (unsigned int) (mst->pos - mst->off),
       (unsigned int) mst->pos,
       (unsigned int) mst->off);
  ret = GNUNET_OK;
  ibuf = (char *) mst->hdr;
  while (mst->pos > 0)
  {
do_align:
    GNUNET_assert (mst->pos >= mst->off);
    if ((mst->curr_buf - mst->off < sizeof(struct GNUNET_MessageHeader)) ||
        (0 != (mst->off % ALIGN_FACTOR)))
    {
      /* need to align or need more space */
      mst->pos -= mst->off;
      memmove (ibuf,
               &ibuf[mst->off],
               mst->pos);
      mst->off = 0;
    }
    if (mst->pos - mst->off < sizeof(struct GNUNET_MessageHeader))
    {
      delta
        = GNUNET_MIN (sizeof(struct GNUNET_MessageHeader)
                      - (mst->pos - mst->off),
                      size);
      GNUNET_memcpy (&ibuf[mst->pos],
                     buf,
                     delta);
      mst->pos += delta;
      buf += delta;
      size -= delta;
    }
    if (mst->pos - mst->off < sizeof(struct GNUNET_MessageHeader))
    {
      if (purge)
      {
        mst->off = 0;
        mst->pos = 0;
      }
      return GNUNET_OK;
    }
    hdr = (const struct GNUNET_MessageHeader *) &ibuf[mst->off];
    want = ntohs (hdr->size);
    LOG (GNUNET_ERROR_TYPE_DEBUG,
         "We want to read message of size %u\n",
         want);
    if (want < sizeof(struct GNUNET_MessageHeader))
    {
      GNUNET_break_op (0);
      return GNUNET_SYSERR;
    }
    if ((mst->curr_buf - mst->off < want) &&
        (mst->off > 0))
    {
      /* can get more space by moving */
      mst->pos -= mst->off;
      memmove (ibuf,
               &ibuf[mst->off],
               mst->pos);
      mst->off = 0;
    }
    if (mst->curr_buf < want)
    {
      /* need to get more space by growing buffer */
      GNUNET_assert (0 == mst->off);
      mst->hdr = GNUNET_realloc (mst->hdr,
                                 want);
      ibuf = (char *) mst->hdr;
      mst->curr_buf = want;
    }
    hdr = (const struct GNUNET_MessageHeader *) &ibuf[mst->off];
    if (mst->pos - mst->off < want)
    {
      delta = GNUNET_MIN (want - (mst->pos - mst->off),
                          size);
      GNUNET_assert (mst->pos + delta <= mst->curr_buf);
      GNUNET_memcpy (&ibuf[mst->pos],
                     buf,
                     delta);
      mst->pos += delta;
      buf += delta;
      size -= delta;
    }
    if (mst->pos - mst->off < want)
    {
      if (purge)
      {
        mst->off = 0;
        mst->pos = 0;
      }
      return GNUNET_OK;
    }
    if (one_shot == GNUNET_SYSERR)
    {
      /* cannot call callback again, but return value saying that
       * we have another full message in the buffer */
      ret = GNUNET_NO;
      goto copy;
    }
    if (one_shot == GNUNET_YES)
      one_shot = GNUNET_SYSERR;
    mst->off += want;
    if (GNUNET_OK !=
        (cbret = mst->cb (mst->cb_cls,
                          hdr)))
    {
      if (GNUNET_SYSERR == cbret)
        GNUNET_log (GNUNET_ERROR_TYPE_WARNING,
                    "Failure processing message of type %u and size %u\n",
                    ntohs (hdr->type),
                    ntohs (hdr->size));
      return GNUNET_SYSERR;
    }
    if (mst->off == mst->pos)
    {
      /* reset to beginning of buffer, it's free right now! */
      mst->off = 0;
      mst->pos = 0;
    }
  }
  GNUNET_assert (0 == mst->pos);
  while (size > 0)
  {
    unsigned long offset = (unsigned long) buf;
    bool need_align = (0 != (offset % ALIGN_FACTOR));

    LOG (GNUNET_ERROR_TYPE_DEBUG,
         "Server-mst has %u bytes left in inbound buffer\n",
         (unsigned int) size);
    if (size < sizeof(struct GNUNET_MessageHeader))
      break;
    if (! need_align)
    {
      /* can try to do zero-copy and process directly from original buffer */
      hdr = (const struct GNUNET_MessageHeader *) buf;
      want = ntohs (hdr->size);
      if (want < sizeof(struct GNUNET_MessageHeader))
      {
        GNUNET_break_op (0);
        mst->off = 0;
        return GNUNET_SYSERR;
      }
      if (size < want)
        break;                  /* or not: buffer incomplete, so copy to private buffer... */
      if (one_shot == GNUNET_SYSERR)
      {
        /* cannot call callback again, but return value saying that
         * we have another full message in the buffer */
        ret = GNUNET_NO;
        goto copy;
      }
      if (GNUNET_YES == one_shot)
        one_shot = GNUNET_SYSERR;
      if (GNUNET_OK !=
          (cbret = mst->cb (mst->cb_cls,
                            hdr)))
      {
        if (GNUNET_SYSERR == cbret)
          GNUNET_log (GNUNET_ERROR_TYPE_WARNING,
                      "Failure processing message of type %u and size %u\n",
                      ntohs (hdr->type),
                      ntohs (hdr->size));
        return GNUNET_SYSERR;
      }
      buf += want;
      size -= want;
    }
    else
    {
      /* need to copy to private buffer to align;
       * yes, we go a bit more spaghetti than usual here */
      goto do_align;
    }
  }
copy:
  if ((size > 0) && (! purge))
  {
    if (size + mst->pos > mst->curr_buf)
    {
      mst->hdr = GNUNET_realloc (mst->hdr,
                                 size + mst->pos);
      ibuf = (char *) mst->hdr;
      mst->curr_buf = size + mst->pos;
    }
    GNUNET_assert (size + mst->pos <= mst->curr_buf);
    GNUNET_memcpy (&ibuf[mst->pos],
                   buf,
                   size);
    mst->pos += size;
  }
  if (purge)
  {
    mst->off = 0;
    mst->pos = 0;
  }
  LOG (GNUNET_ERROR_TYPE_DEBUG,
       "Server-mst leaves %u (%u/%u) bytes in private buffer\n",
       (unsigned int) (mst->pos - mst->off),
       (unsigned int) mst->pos,
       (unsigned int) mst->off);
  return ret;
}

static unsigned int
check_for_queue_with_higher_prio (struct Queue *queue, struct Queue *queue_head)
{
  for (struct Queue *s = queue_head; NULL != s;
       s = s->next_client)
  {
  println("Yeah");
  }
  return 0;
 }


struct GNUNET_NAMESTORE_QueueEntry *
GNUNET_NAMESTORE_records_store (
  struct GNUNET_NAMESTORE_Handle *h,
  const struct GNUNET_CRYPTO_BlindablePrivateKey *pkey,
  unsigned int rd_set_count,
  const struct GNUNET_NAMESTORE_RecordInfo *record_info,
  unsigned int *rds_sent,
  GNUNET_NAMESTORE_ContinuationWithStatus cont,
  void *cont_cls)
{
  struct GNUNET_NAMESTORE_QueueEntry *qe;
  struct GNUNET_MQ_Envelope *env;
  const char *label;
  unsigned int rd_count;
  const struct GNUNET_GNSRECORD_Data *rd;
  char *name_tmp;
  char *rd_ser;
  ssize_t rd_ser_len[rd_set_count];
  size_t name_len;
  uint32_t rid;
  struct RecordStoreMessage *msg;
  struct RecordSet *rd_set;
  ssize_t sret;
  int i;
  size_t rd_set_len = 0;
  size_t key_len = 0;
  size_t max_len;
  key_len = GNUNET_CRYPTO_blindable_sk_get_length (pkey);
  max_len = UINT16_MAX - key_len - sizeof (struct RecordStoreMessage);

  *rds_sent = 0;
  for (i = 0; i < rd_set_count; i++)
  {
    label = record_info[i].a_label;
    rd_count = record_info[i].a_rd_count;
    rd = record_info[i].a_rd;
    name_len = strlen (label) + 1;
    if (name_len > MAX_NAME_LEN)
    {
      GNUNET_break (0);
      *rds_sent = 0;
      return NULL;
    }
    rd_ser_len[i] = GNUNET_GNSRECORD_records_get_size (rd_count, rd);
    if (rd_ser_len[i] < 0)
    {
      GNUNET_break (0);
      *rds_sent = 0;
      return NULL;
    }
    if (rd_ser_len[i] > max_len)
    {
      GNUNET_break (0);
      *rds_sent = 0;
      return NULL;
    }
    if ((rd_set_len + sizeof (struct RecordSet) + name_len + rd_ser_len[i]) >
        max_len)
      break;
    rd_set_len += sizeof (struct RecordSet) + name_len + rd_ser_len[i];
  }
  *rds_sent = i;
  GNUNET_log (GNUNET_ERROR_TYPE_DEBUG,
              "Sending %u of %u records!\n", *rds_sent, rd_set_count);
  rid = get_op_id (h);
  qe = GNUNET_new (struct GNUNET_NAMESTORE_QueueEntry);
  qe->h = h;
  qe->cont = cont;
  qe->cont_cls = cont_cls;
  qe->op_id = rid;
  GNUNET_CONTAINER_DLL_insert_tail (h->op_head, h->op_tail, qe);
  /* setup msg */
  env = GNUNET_MQ_msg_extra (msg,
                             key_len + rd_set_len,
                             GNUNET_MESSAGE_TYPE_NAMESTORE_RECORD_STORE);
  GNUNET_assert (NULL != msg);
  GNUNET_assert (NULL != env);
  msg->gns_header.r_id = htonl (rid);
  msg->key_len = htons (key_len);
  msg->single_tx = htons (GNUNET_YES);
  msg->rd_set_count = htons ((uint16_t) (*rds_sent));
  GNUNET_CRYPTO_write_blindable_sk_to_buffer (pkey,
                                              &msg[1],
                                              key_len);
  rd_set = (struct RecordSet*) (((char*) &msg[1]) + key_len);
  for (i = 0; i < *rds_sent; i++)
  {
    label = record_info[i].a_label;
    rd = record_info[i].a_rd;
    name_len = strlen (label) + 1;
    rd_set->name_len = htons (name_len);
    rd_set->rd_count = htons (record_info[i].a_rd_count);
    rd_set->rd_len = htons (rd_ser_len[i]);
    rd_set->reserved = ntohs (0);
    name_tmp = (char *) &rd_set[1];
    GNUNET_memcpy (name_tmp, label, name_len);
    rd_ser = &name_tmp[name_len];
    sret = GNUNET_GNSRECORD_records_serialize (record_info[i].a_rd_count,
                                               rd, rd_ser_len[i], rd_ser);
    if ((0 > sret) || (sret != rd_ser_len[i]))
    {
      GNUNET_break (0);
      GNUNET_free (env);
      return NULL;
    }
    // Point to next RecordSet
    rd_set = (struct RecordSet*) &name_tmp[name_len + rd_ser_len[i]];
  }
  LOG (GNUNET_ERROR_TYPE_DEBUG,
       "Sending NAMESTORE_RECORD_STORE message for name %u record sets\n",
       *rds_sent);
  qe->timeout_task =
    GNUNET_SCHEDULER_add_delayed (NAMESTORE_DELAY_TOLERANCE, &warn_delay, qe);
  if (NULL == h->mq)
  {
    qe->env = env;
    LOG (GNUNET_ERROR_TYPE_WARNING,
         "Delaying NAMESTORE_RECORD_STORE message as namestore is not ready!\n")
    ;
  }
  else
  {
    GNUNET_MQ_send (h->mq, env);
  }
  return qe;
}