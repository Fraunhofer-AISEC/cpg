import numpy as np
from qiskit import QuantumCircuit, transpile
from qiskit.providers.aer import QasmSimulator
from qiskit.visualization import plot_histogram
simulator = QasmSimulator()
# Some values from the classical world
a = 0
b = 1
circuit = QuantumCircuit(3, 3) # Create a Quantum Circuit with 3 qubits
# initialize qubits
circuit.initialize(str(a), 0)
circuit.initialize(str(b), 1)
circuit.initialize(str(b), 2)
circuit.h(0) # Add a H gate on qubit 0
circuit.cx(0, 1) # Add a CX (CNOT) gate on control qubit 0 and target qubit 1
circuit.measure([0,1,2], [0,1,2]) # Map the quantum measurement to the classical bits
compiled_circuit = transpile(circuit, simulator)
job = simulator.run(compiled_circuit, shots=1000)
# Grab results from the job
result = job.result()
# Returns counts
counts = result.get_counts(compiled_circuit)
# Results back to classical word
for bitstring in counts:
    if counts[bitstring] > 300: # some basic threshold to ignore noise
        d = int(bitstring[-1])
        e = int(bitstring[-2])
        #f = int(bitstring[-3])
        print("In this solution variable: d = %d and e = %d" % (d, e, f))
