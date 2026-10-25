void caller() {
  plain_global(1);
  plain_global(2);
  plain_global(3);
  missing::ns::qualified_fn("a");
  missing::ns::qualified_fn("b");
  missing::ns::qualified_fn("c");
}
