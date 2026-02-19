OPENQASM 3.0;
include "qelib1.inc";

qreg q[4];
creg c[4];

y q[1];
z q[2];
t q[0];
tdg q[1];
s q[2];
sdg q[3];

measure q[0]->c[0];
measure q[2]->c[1];
measure q[3]->c[2];
measure q[1]->c[3];