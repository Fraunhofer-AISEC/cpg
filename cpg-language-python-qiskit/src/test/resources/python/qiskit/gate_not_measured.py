from qiskit import QuantumCircuit, transpile
from qiskit.providers.aer import QasmSimulator
from qiskit.visualization import plot_histogram
simulator = QasmSimulator()
circuit = QuantumCircuit(3, 2)
circuit.h(0)
circuit.cx(1, 0)
circuit.h(2)
circuit.measure([1,2], [0,1])
compiled_circuit = transpile(circuit, simulator)
job = simulator.run(compiled_circuit, shots=1000)
result = job.result()
counts = result.get_counts(compiled_circuit)
print(circuit.draw())
print(circuit.draw(output='latex_source'))