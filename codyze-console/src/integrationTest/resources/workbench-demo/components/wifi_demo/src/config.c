#include "../include/config.h"
#include "../include/log.h"

static int is_valid_ssid(const char *ssid) {
    return ssid[0] != 0;
}

wifi_config_t load_config(void) {
    wifi_config_t cfg;
    cfg.secure = 1;
    cfg.debug = 0;
    cfg.retries = 3;
    cfg.on_connect = 0;
    return cfg;
}

int validate_config(wifi_config_t *cfg) {
    if (!is_valid_ssid(cfg->ssid)) {
        log_error("invalid ssid");
        return 0;
    }
    if (cfg->retries > 5) {
        cfg->retries = 5;
    }
    return 1;
}
