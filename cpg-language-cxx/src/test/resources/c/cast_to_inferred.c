int foo(const mytype *p)
{
	mytype *v;
	*v = (mytype)*p;
	return 0;
}