import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random


data class EntangledState(
    val zerosAmplitude: Double,
    val onesAmplitude: Double,
    val zerosVector: MutableList<Int>,
    val onesVector: MutableList<Int>
)


class QuantumTuringMachine {
    // 2 bits for num A, 2 bits for num B, and 3 bits for result
    // 3 + 3 = 6
    val tape = List(7) { QuantumBit() }.toMutableList()
    private val binaryTape = intArrayOf(1, 1, 1, 1, 0, 0, 0)
    private var headPosition = 0
    private lateinit var returnList: MutableList<QuantumBit>

    private fun step() {
        headPosition++
    }

    private fun qubitAdd(a0: QuantumBit, a1: QuantumBit, b0: QuantumBit, b1: QuantumBit): MutableList<QuantumBit> {
        val c0 = QuantumBit()
        val c1 = QuantumBit()
        val c2 = QuantumBit()
        val carry0 = QuantumBit()

        if (a0.oneAmplitude == 1.0 && b0.oneAmplitude == 1.0) {
            carry0.applyXOR()
        } else if (a0.oneAmplitude == 1.0 || b0.oneAmplitude == 1.0) {
            c0.applyXOR()
        } else {
            // both a0 and b0 is 0 so ignore
        }

        if (a1.oneAmplitude == 1.0 && b1.oneAmplitude == 1.0 && carry0.oneAmplitude == 1.0) {
            c2.applyXOR()
            c1.applyXOR()
        } else if (a1.oneAmplitude == 1.0 && b1.oneAmplitude == 1.0 && carry0.oneAmplitude == 0.0) {
            c2.applyXOR()
        } else if (a1.oneAmplitude == 1.0 || b1.oneAmplitude == 1.0 && carry0.oneAmplitude == 1.0) {
            c2.applyXOR()
        } else if (a1.oneAmplitude == 0.0 && b1.oneAmplitude == 0.0 && carry0.oneAmplitude == 1.0) {
            c1.applyXOR()
        } else if (a1.oneAmplitude == 1.0 || b1.oneAmplitude == 1.0 && carry0.oneAmplitude == 0.0) {
            c1.applyXOR()
        } else {
            // a1 and b1 and carry0 is 0 so ignore
        }


        return mutableListOf(c2, c1, c0)
    }


    fun run() {
        while (headPosition < tape.size) {
            // Record what the machine sees
            if (binaryTape[headPosition] == 0) {
                tape[headPosition].zeroAmplitude = 1.0
                tape[headPosition].oneAmplitude = 0.0
            } else {
                tape[headPosition].zeroAmplitude = 0.0
                tape[headPosition].oneAmplitude = 1.0
            }

            if (headPosition == 3) {
                val a0 = tape[1]
                val a1 = tape[0]
                val b0 = tape[3]
                val b1 = tape[2]

                returnList = qubitAdd(a0, a1, b0, b1)
            }

            if (headPosition == 4) {
                tape[headPosition] = returnList[0]
            }
            if (headPosition == 5) {
                tape[headPosition] = returnList[1]
            }
            if (headPosition == 6) {
                tape[headPosition] = returnList[2]
            }

            step()
        }
    }
}


class GHZSystem {
    private lateinit var entangledState: EntangledState

    override fun toString(): String {
        val formattedOneAmplitude = String.format("%.3f", entangledState.onesAmplitude).trimEnd('0')
        val formattedZeroAmplitude = String.format("%.3f", entangledState.zerosAmplitude).trimEnd('0')

        return when {
            formattedOneAmplitude.startsWith("0.707") && formattedZeroAmplitude.startsWith("0.707") -> {
                "1/√2(|${"0".repeat(entangledState.zerosVector.size)}> + |${"1".repeat(entangledState.onesVector.size)}>)"
            }

            formattedOneAmplitude.startsWith("-0.707") && formattedZeroAmplitude.startsWith("0.707") -> {
                "1/√2(|${"0".repeat(entangledState.zerosVector.size)}> - |${"1".repeat(entangledState.onesVector.size)}>)"
            }

            formattedOneAmplitude.startsWith("0.707") && formattedZeroAmplitude.startsWith("-0.707") -> {
                "(-1/√2|${"0".repeat(entangledState.zerosVector.size)}> + 1/√2|${"1".repeat(entangledState.onesVector.size)}>)"
            }

            formattedOneAmplitude.startsWith("-0.707") && formattedZeroAmplitude.startsWith("-0.707") -> {
                "-1/√2(|${"0".repeat(entangledState.zerosVector.size)}> + |${"1".repeat(entangledState.onesVector.size)}>)"
            }

            formattedOneAmplitude == formattedZeroAmplitude -> {
                "${formattedZeroAmplitude}(|${"0".repeat(entangledState.zerosVector.size)}> + |${
                    "1".repeat(
                        entangledState.onesVector.size
                    )
                }>)"
            }

            else -> {
                "(${formattedZeroAmplitude}|${"0".repeat(entangledState.zerosVector.size)}> + ${formattedOneAmplitude}|${
                    "1".repeat(
                        entangledState.onesVector.size
                    )
                }>)"
            }
        }
    }

    fun createGHZStateFromSystem(originalSystem: QuantumSystem) {
        val qubitList = originalSystem.qubits

        for (i in 0..<qubitList.size) {
            if (i == 0) {
                val originalQubit = qubitList[0]
                originalQubit.applyHadamard()
                entangledState = EntangledState(
                    zerosAmplitude = originalQubit.zeroAmplitude,
                    onesAmplitude = originalQubit.oneAmplitude,
                    zerosVector = mutableListOf(0),
                    onesVector = mutableListOf(1)
                )
                continue
            }

            //qubits not in superposition
            val currentQubit = qubitList[i]
            currentQubit.applyCNOT(qubitList[0])
            if (currentQubit.zeroAmplitude == 1.0) {
                entangledState.zerosVector.add(1)
                entangledState.onesVector.add(0)

            } else {
                entangledState.zerosVector.add(0)
                entangledState.onesVector.add(1)
            }

        }
    }

    fun measureGHZState(): String {
        val probabilityOfOne = entangledState.onesAmplitude * entangledState.onesAmplitude
        val probabilityOfZero = entangledState.zerosAmplitude * entangledState.zerosAmplitude
        if (abs(probabilityOfZero + probabilityOfOne - 1) > 0.001) throw IllegalArgumentException(
            "GHZ state amplitudes do not compute to 1! GHZ State: $entangledState"
        )
        return if (Math.random() < probabilityOfOne) {
            "|${"1".repeat(entangledState.onesVector.size)}>"
        } else {
            "|${"0".repeat(entangledState.zerosVector.size)}>"
        }
    }
}


class QuantumSystem {
    var qubits = mutableListOf<QuantumBit>()

    fun addQubit(qubit: QuantumBit) {
        qubits.add(qubit)
    }

    fun addQubitAmount(amount: Int) {
        for (i in 0..<amount) {
            qubits.add(QuantumBit())
        }
    }

    private fun measureQubit(qubitIndex: Int): Int {
        val qubit = qubits[qubitIndex]
        val probabilityOfOne = qubit.oneAmplitude * qubit.oneAmplitude
        val probabilityOfZero = qubit.zeroAmplitude * qubit.zeroAmplitude
        if (abs(probabilityOfZero + probabilityOfOne - 1) > 0.001) throw IllegalArgumentException(
            "Qubit amplitudes do not compute to 1! Qubit: $qubit at index: $qubitIndex"
        )
        return if (Math.random() < probabilityOfOne) 1 else 0
    }

    fun measureAllQubits(): List<Int> {
        return qubits.map { measureQubit(qubits.indexOf(it)) }
    }

    fun copy(): QuantumSystem {
        val copy = QuantumSystem()
        for (qubit in qubits) {
            copy.qubits.add(qubit.copy())
        }
        return copy
    }
}


class QuantumBit(
    // By default, all newly created Qubits are initialized to |0>
    var zeroAmplitude: Double = 1.0,
    var oneAmplitude: Double = 0.0,
    // |ψ> = 1|0> + 0|1>
) {

    var funcMultiplier: String? = null

    companion object {
        fun applyCNOTAndCast(target: QuantumBit, control: QuantumBit): QuantumBit {
            // The CNOT gate flips the target's state only when the control is definitely in the |1> state.
            // This implementation checks if the probability of the control being in |1> is greater than 50%
            if (control.oneAmplitude * control.oneAmplitude > 0.5) {
                val temp = target.zeroAmplitude
                target.zeroAmplitude = target.oneAmplitude
                target.oneAmplitude = temp
            }
            return target  // Returning the target is more typical for operations modifying the target.
        }

        fun applyCCNOTAndCast(target: QuantumBit, control1: QuantumBit, control2: QuantumBit): QuantumBit {
            // The CCNOT gate flips the target qubit only if both control qubits are in the |1> state
            // Since we are working with amplitude probabilities, we check if the amplitude of |1> state is non-zero for both controls
            if (control1.oneAmplitude != 0.0 && control2.oneAmplitude != 0.0) {
                // Swap the amplitudes of |0> and |1> for the target qubit
                val tempZero = target.zeroAmplitude
                target.zeroAmplitude = target.oneAmplitude
                target.oneAmplitude = tempZero
            }
            return target
        }
    }

    override fun toString(): String {
        val formattedOneAmplitude = String.format("%.3f", oneAmplitude).trimEnd('0')
        val formattedZeroAmplitude = String.format("%.3f", zeroAmplitude).trimEnd('0')

        if (funcMultiplier == null) {
            funcMultiplier = ""
        }

        return when {
            oneAmplitude == 0.0 -> {
                "$funcMultiplier$zeroAmplitude|0>"
            }

            zeroAmplitude == 0.0 -> {
                "$funcMultiplier$oneAmplitude|1>"
            }

            formattedOneAmplitude.startsWith("0.707") && formattedZeroAmplitude.startsWith("0.707") -> {
                "$funcMultiplier|+>"
                //"$funcMultiplier1/√2(|0> + |1>)"
            }

            formattedOneAmplitude.startsWith("-0.707") && formattedZeroAmplitude.startsWith("0.707") -> {
                "$funcMultiplier|->"
                //"$funcMultiplier(1/√2|0> + -1/√2|1>)"
            }

            formattedOneAmplitude.startsWith("0.707") && formattedZeroAmplitude.startsWith("-0.707") -> {
                "$funcMultiplier(-1/√2|0> + 1/√2|1>)"
            }

            formattedOneAmplitude.startsWith("-0.707") && formattedZeroAmplitude.startsWith("-0.707") -> {
                "$funcMultiplier-1/√2(|0> + |1>)"
            }

            formattedOneAmplitude == formattedZeroAmplitude -> {
                "$funcMultiplier$formattedOneAmplitude(|0> + |1>)"
            }

            else -> {
                "$funcMultiplier($formattedZeroAmplitude|0> + $formattedOneAmplitude|1>)"
            }
        }
    }

    fun applyHadamard() {
        if (zeroAmplitude == 0.0 || oneAmplitude == 0.0) {
            val newZeroAmplitude = (zeroAmplitude + oneAmplitude) / sqrt(2.0)
            val newOneAmplitude = (zeroAmplitude - oneAmplitude) / sqrt(2.0)
            zeroAmplitude = newZeroAmplitude
            oneAmplitude = newOneAmplitude
            return
        }
    }

    fun applyCNOT(control: QuantumBit) {
        // Assuming control qubit's influence when its |1> amplitude squared (probability) is greater than 0.5
        if (control.oneAmplitude * control.oneAmplitude > 0.5) {
            val temp = zeroAmplitude
            zeroAmplitude = oneAmplitude
            oneAmplitude = temp
        }
    }

    fun applyCCNOT(control1: QuantumBit, control2: QuantumBit) {
        if (control1.oneAmplitude != 0.0 && control2.oneAmplitude != 0.0) {
            val temp = zeroAmplitude
            zeroAmplitude = oneAmplitude
            oneAmplitude = temp
        }
    }

    fun applyXOR() {
        val temp = zeroAmplitude
        zeroAmplitude = oneAmplitude
        oneAmplitude = temp
    }

    fun copy(): QuantumBit {
        return QuantumBit(zeroAmplitude, oneAmplitude)
    }

    fun applyOracleUf(): Int {
        this.funcMultiplier = "U_f"

        val randomValue = Random.nextDouble()
        val type = when {
            randomValue < 0.25 -> 1
            randomValue < 0.50 -> 2
            randomValue < 0.75 -> 3
            else -> 4
        }

        //1: constant 0
        //2: constant 1
        //3: balanced, 1 for 1
        //4: balanced, 1 for 0

        return when (type) {
            1 -> 0
            2 -> 1
            3 -> 1
            4 -> 0
            else -> 0
        }
    }
}

fun main() {

    val v1 = QuantumBit()
    val v2 = QuantumBit()

    // Circuit visualization initial
    //                    +-+    |------|    +-+     +-------+
    // v1: |0> ----------|H |----|      |---|H |--- |Measure |---
    //                   +-+     | U_f  |   +-+     +-------+
    //             +-+    +-+    |      |
    // v2: |0> ---|X |---|H |----|      |------------------------
    //            +-+    +-+     |------|

    // Step 1
    v2.applyXOR()

    print("v1: $v1, v2: $v2\n")

    // Step 2
    v1.applyHadamard()
    v2.applyHadamard()

    print("v1: $v1, v2: $v2\n")

    // Circuit visualization Step 2
    //                    +-+  |  |------|    +-+     +-------+
    // v1: |0> ----------|H |--|--|      |---|H |--- |Measure |---
    //                   +-+   |  | U_f  |   +-+     +-------+
    //             +-+    +-+  |  |      |
    // v2: |0> ---|X |---|H |--|--|      |------------------------
    //            +-+    +-+   |  |------|
    //                         |
    //                        Here
    //               Now we have v1 in the plus state
    //               (|+>) and v2 in the minus state (|->)

    // Step 3
    v1.applyOracleUf()
    v2.applyOracleUf()

    print("v1: $v1, v2: $v2\n")

    // Circuit visualization step 3
    //                    +-+    |------|  |  +-+     +-------+
    // v1: |0> ----------|H |----|      |--|-|H |--- |Measure |---
    //                   +-+     | U_f  |  | +-+     +-------+
    //             +-+    +-+    |      |  |
    // v2: |0> ---|X |---|H |----|      |--|----------------------
    //            +-+    +-+     |------|  |
    //                                     |
    //                                    Here
    //              We can represent v1 as 1/√2(|0> + |1>) and v2 as |->, giving
    //              us 1/√2(|0>|-> + |1>|->) after distribution. After the oracle
    //              is applied the function will look like this:
    //              U_f*1/√2(|0>|-> + |1>|->) -> 1/√2(U_f|0>|-> + U_f|1>|->)
    //              since U_f|x>|-> = (-1)^(f(x))*|x>|->, we can rewrite our equation
    //              as 1/√2((-1)^(f(0))|0>|-> + (-1)^(f(1))|1>|)

    // Step 4
    // In this step we do the calculation
    val blackBoxAnswer = v1.applyOracleUf()
    v1.funcMultiplier = null

    // if f(0) == f(1): +-1/√2(|0> + |1>)
    // if f(0) != f(1): +-1/√2(|0> - |1>)

    // Step 5
    v1.applyHadamard()
    if (blackBoxAnswer == 1) {
        v1.zeroAmplitude = 1.0
        v1.oneAmplitude = 0.0
    } else {
        v1.zeroAmplitude = 0.0
        v1.oneAmplitude = 1.0
    }

    // Result
    if (v1.zeroAmplitude == 1.0) {
        println("Function is constant")
    } else {
        println("Function is balanced")
    }


    // Beginning of Simon's Algorithm

}


// Main function for HW2
//fun main() {
//
//    // Init everything
//    val system = QuantumSystem()
//    val ghz = GHZSystem()
//    val qtm = QuantumTuringMachine()
//
//    // Add qubits into system
//    system.addQubitAmount(10)
//    // Create GHZ state from added qubits
//    ghz.createGHZStateFromSystem(system.copy())
//
//    // Measure and collapse  the GHZ state into a result
//    println("GHZ State: ${ghz.measureGHZState()}")
//
//    // Run the turing machine over the tape
//    qtm.run()
//
//    // Show turing machine results
//    println("Quantum tape: ${qtm.tape}")
//
//
//    // Measure the GHZ state and record the result x amount of times
//    val x = 100
//    var zerosCount = 0
//    var onesCount = 0
//    for (i in 0..x) {
//        if (ghz.measureGHZState()[1] == '0') {
//            zerosCount++
//        } else {
//            onesCount++
//        }
//    }
//    println("After $x measurements, the results were:")
//    println("Ones: $onesCount, Zeros: $zerosCount")
//
//}
