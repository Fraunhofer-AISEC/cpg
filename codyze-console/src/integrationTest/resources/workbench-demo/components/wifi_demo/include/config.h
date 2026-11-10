#ifndef CONFIG_H
#define CONFIG_H

typedef struct wifi_config {
    char ssid[32];
    char password[64];
    int secure;
    int debug;
    int retries;
    /* A function pointer: the analysis cannot know what is called through it */
    void (*on_connect)(const char *key);
} wifi_config_t;

wifi_config_t load_config(void);
int validate_config(wifi_config_t *cfg);

#endif
