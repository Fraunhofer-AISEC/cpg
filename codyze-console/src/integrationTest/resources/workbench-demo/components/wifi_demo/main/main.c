#include "../include/config.h"
#include "../include/crypto.h"
#include "../include/log.h"
#include "../include/net.h"

static int sock;

/* The key is written to the log: a leak, but only if debug is enabled */
static void report_key(const char *key) {
    log_debug(key);
}

static int send_with_retries(const char *buf, int len) {
    for (int i = 0; i < 3; i++) {
        if (net_send(sock, buf, len) > 0) {
            return 1;
        }
        log_error("send failed");
    }
    return 0;
}

static const char *describe(int retries) {
    switch (retries) {
        case 0:
            return "no retries";
        case 1:
            return "one retry";
        default:
            return "several retries";
    }
}

void app_main(void) {
    wifi_config_t cfg = load_config();
    if (!validate_config(&cfg)) {
        return;
    }

    /* get_key is not declared anywhere: the analysis does not know what it returns (external) */
    char *key = get_key(&cfg);
    char buf[64];
    int len = sizeof(buf);

    if (cfg.secure) {
        encrypt(key, buf, len);
    } else {
        copy_plain(key, buf, len);
    }

    if (cfg.debug) {
        report_key(key);
    }

    /* The target of a call through a function pointer cannot be resolved */
    cfg.on_connect(key);

    log_debug(describe(cfg.retries));
    sock = net_open();
    send_with_retries(buf, len);
}
