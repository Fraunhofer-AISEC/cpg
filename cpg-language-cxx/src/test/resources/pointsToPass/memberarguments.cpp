static c_t testMemberArg(const creds_t *c) {
  conf_t conf;
  memset(conf.st.s, UNIT8_MAX, sizeof(conf.st.s));
  printf("%d", conf);
  strlcpy((char *)conf.st.s, c->ssid, sizeof(conf.st.s))
  printf("%d", conf, conf.st);
  return &conf;
}
