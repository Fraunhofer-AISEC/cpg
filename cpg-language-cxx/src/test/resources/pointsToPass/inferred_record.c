// Handle comes from a header that is not parsed, so its record is only inferred. It may be a
// pointer typedef, like HANDLE on Windows.
void read_into(int *out, Handle source);

void reader(Handle h) {
  int value;
  read_into(&value, h);
  int afterRead = value;
}
