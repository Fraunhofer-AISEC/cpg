// Reduced from esp_mmu_paddr_find_caps as decompiled by Ghidra (ESP-IDF, esp_mm). The outer loop
// advances the pointer to a field of the object it currently points to, so every iteration reaches
// a new field address.
typedef struct mem_block_ mem_block_;

struct entries_t {
  mem_block_ *tqe_next;
  mem_block_ **tqe_prev;
};

struct mem_block_ {
  unsigned int laddr_start;
  unsigned int paddr_start;
  unsigned int paddr_end;
  int caps;
  struct entries_t entries;
};

typedef struct {
  mem_block_ *tqh_first;
  mem_block_ **tqh_last;
} mem_block_head_t;

typedef struct {
  mem_block_head_t mem_block_head;
} mem_region_t;

typedef struct {
  unsigned int num_regions;
  mem_region_t mem_regions[4];
} mmu_ctx_t;

mmu_ctx_t s_mmu_ctx;

int find_caps(unsigned int paddr, int *out_caps) {
  int found;
  unsigned int i;
  mem_block_ *block;
  mem_block_ *head;
  mem_block_ *found_block;
  mmu_ctx_t *ctx;

  if (out_caps == (int *)0x0) {
    return 0x102;
  }
  ctx = &s_mmu_ctx;
  found_block = (mem_block_ *)0x0;
  found = 0;
  for (i = 0; i != s_mmu_ctx.num_regions; i = i + 1) {
    head = ctx->mem_regions[0].mem_block_head.tqh_first;
    for (block = head; block != (mem_block_ *)0x0; block = (block->entries).tqe_next) {
      if (block != head && block->paddr_start <= paddr && paddr < block->paddr_end) {
        found = 1;
        found_block = block;
        break;
      }
    }
    ctx = (mmu_ctx_t *)&ctx->mem_regions[0].mem_block_head.tqh_last;
  }
  if (found) {
    *out_caps = found_block->caps;
    return 0;
  }
  return 0x105;
}
