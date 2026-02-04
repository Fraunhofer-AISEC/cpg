# Source: https://qiskit.org/documentation/intro_tutorial1.html

import numpy as np
from qiskit import QuantumCircuit, transpile
from qiskit.providers.aer import QasmSimulator
from qiskit.visualization import plot_histogram

def do_something_complex():
    print("very complex")

# Use Aer's qasm_simulator
simulator = QasmSimulator()

# Some values defined in the local world
a = 0; b = 1
# Create a quantum circuit with 4 qubits
circuit = QuantumCircuit(4, 4)
# Initialize qubits with local world values
circuit.initialize(str(a), 0)
circuit.initialize(str(a), 1)
circuit.initialize(str(b), 2)
circuit.initialize(str(b), 3)
# Add a H gate on qubit 0 and qubit 3
circuit.h(0); circuit.h(3)
# Add a CX (CNOT) gate on control qubit 1
# and target qubit 0
circuit.cx(1, 0)
# Add a X gate on qubit 1 if qubit 2 is 0
circuit.measure([2], [2])
circuit.x(1).c_if(2, 0)
# Measure the remaining qubits
circuit.measure([1,3], [1,3])
# Run job
cc = transpile(circuit, simulator)
job = simulator.run(cc, shots=1000)
# Grab results from the job
result = job.result()
counts = result.get_counts(cc)
# Evaluate results back in local world
for bitstring in counts:
  # Some basic threshold to ignore noise
  if counts[bitstring] > 300:
    c0 = int(bitstring[-1])
    c1 = int(bitstring[-2])
    c2 = int(bitstring[-3])
    c3 = int(bitstring[-4])
    print("c1=%d,c2=%d,c3=%d,c4=%d" %
        (c0, c1 ,c2, c3))
    # Execute code based on a decision
    # of a measured bit
    if c2 == 1:
      do_something_complex()

# Draw the circuit
print(circuit.draw(output='latex_source'))
