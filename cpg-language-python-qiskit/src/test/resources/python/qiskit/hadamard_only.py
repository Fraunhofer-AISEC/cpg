# Source: https://qiskit.org/documentation/intro_tutorial1.html

import numpy as np
from qiskit import QuantumCircuit, transpile
from qiskit.providers.aer import QasmSimulator
from qiskit.visualization import plot_histogram

# Use Aer's qasm_simulator
simulator = QasmSimulator()

# Create a Quantum Circuit acting on the q register
circuit = QuantumCircuit(1, 1)

# Add a H gate on qubit 0
circuit.h(0)

# Map the quantum measurement to the classical bits
circuit.measure([0], [0])

# compile the circuit down to low-level QASM instructions
# supported by the backend (not needed for simple circuits)
compiled_circuit = transpile(circuit, simulator)

# Execute the circuit on the qasm simulator
job = simulator.run(compiled_circuit, shots=1000)

# Grab results from the job
result = job.result()

# Returns counts
counts = result.get_counts(compiled_circuit)
print("\nTotal count for 0 and 1 are:",counts)

# Draw the circuit
print(circuit.draw(output='latex_source'))
