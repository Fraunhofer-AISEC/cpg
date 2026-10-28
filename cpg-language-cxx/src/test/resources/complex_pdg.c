#include <stdio.h>
#include <stdlib.h>

typedef struct {
    int value;
    int valid;
} Item;

/*
 * Interesting for PDGs:
 * - 'x' and '*out' create data dependencies across a call boundary.
 * - The branch creates control dependence on 'x > 0'.
 */
static int transform(int x, int *out)
{
    int tmp;

    if (x > 0) {
        tmp = x * 2;          // Data: tmp depends on x.
        *out = tmp + 1;       // Data: out depends on tmp.
    } else {
        tmp = -x;             // Control-dependent on x <= 0.
        *out = tmp - 1;       // Data dependency through tmp.
    }

    return tmp;               // Depends on both branch definitions of tmp.
}

/*
 * Main PDG playground.
 */
int process(Item *items, size_t n, int threshold)
{
    int sum = 0;
    int checksum = 0;
    int state = 0;
    int *alias = &sum;        // 'alias' may indirectly modify sum.
    int transformed;

    for (size_t i = 0; i < n; ++i) {
        Item *cur = &items[i];

        /*
         * Control dependence:
         * everything in this block depends on cur->valid.
         *
         * Data dependence:
         * cur itself depends on i and items.
         */
        if (!cur->valid)
            continue;         // Creates a non-local control-flow edge.

        /*
         * Multiple definitions of state make reaching-definitions
         * and φ-like merging interesting in the PDG.
         */
        if (cur->value > threshold) {
            state = 1;
            sum += cur->value; // sum depends on previous sum + value.
        } else {
            state = -1;
            sum -= cur->value;
        }

        /*
         * Function-call dependence:
         * transformed depends on cur->value;
         * transform() also modifies sum through alias.
         */
        transformed = transform(cur->value, &checksum);

        /*
         * Data dependency crosses the call:
         * checksum was defined inside transform().
         */
        if (transformed > 10) {
            checksum += state; // Depends on branch-selected state.
        }

        /*
         * Alias-sensitive dependence:
         * alias points to sum, so this statement modifies the same
         * abstract memory location as sum += / sum -= above.
         */
        if ((i & 1) == 0) {
            *alias += checksum;
        }

        /*
         * Control dependence on transformed and state.
         * The resulting checksum influences a later exit.
         */
        if (state < 0 && checksum > 100)
            break;             // Loop-carried + control dependence.
    }

    /*
     * Loop-carried dependency:
     * sum/checksum/state may have values originating from any
     * previous iteration.
     */
    if (sum != 0) {
        checksum ^= sum;
    } else {
        checksum += n;
    }

    /*
     * Final value depends on several independent control regions.
     */
    return checksum + state;
}

int main(void)
{
    Item data[] = {
        { 12, 1 },
        { -4, 1 },
        { 20, 0 },
        { 7,  1 },
        { 30, 1 }
    };

    int result = process(data, 5, 10);

    printf("result = %d\n", result);

    return EXIT_SUCCESS;
}
