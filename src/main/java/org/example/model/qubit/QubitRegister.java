package org.example.model.qubit;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Random;

public class QubitRegister implements Serializable {
    private final /* final */ int numQubits;

    @Getter
    @Setter
    private BitSet states;

    @Getter
    @Setter
    private ChunkedComplexArray amplitudes;

    @Getter
    private final String name;
    private final Random random;

    @Getter
    private final QubitRegister realRegister;
    @Getter
    private final Integer offsetInRealRegister;

    public QubitRegister(String name, int numQubits) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Имя регистра не может быть пустым");
        }
        if (numQubits <= 0) {
            throw new IllegalArgumentException("Количество кубитов должно быть положительным");
        }
        this.name = name;
        this.numQubits = numQubits;
        this.states = new BitSet(1 << numQubits);
        this.states.set(0, true);
        this.amplitudes = new ChunkedComplexArray();
        this.amplitudes.set(0, Complex.getOne());
        this.random = new Random();
        this.realRegister = this;
        this.offsetInRealRegister = 0;
    }

    public QubitRegister(String name, int numQubits, QubitRegister realRegister, int offsetInRealRegister) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Имя номинального регистра не может быть пустым");
        }
        if (numQubits <= 0) {
            throw new IllegalArgumentException("Количество кубитов номинального регистра должно быть положительным");
        }
        if (realRegister == null) {
            throw new IllegalArgumentException("Ссылка на реальный регистр не может быть null");
        }
        if (offsetInRealRegister < 0) {
            throw new IllegalArgumentException("Смещение в реальном регистре не может быть отрицательным");
        }
        if (offsetInRealRegister + numQubits > realRegister.size()) {
            throw new IllegalArgumentException("Номинальный регистр " + name + " выходит за границы реального регистра " + realRegister.getName());
        }

        this.name = name;
        this.numQubits = numQubits;
        this.realRegister = realRegister;
        this.offsetInRealRegister = offsetInRealRegister;

        this.states = null;
        this.amplitudes = null;
        this.random = null;
    }

    public int size() {
        return numQubits;
    }

    public int getRealSize() {
        return this.realRegister.numQubits;
    }

    public BitSet getStates() {
        return this.realRegister.states;
    }

    public ChunkedComplexArray getAmplitudes() {
        return this.realRegister.amplitudes;
    }

    public Integer sampleQubit(int nominalIndex) {
        if (nominalIndex < 0 || nominalIndex >= this.numQubits) {
            throw new IndexOutOfBoundsException("Индекс " + nominalIndex + " вне границ номинального регистра " + name + " размера " + numQubits);
        }
        int realIndex = this.offsetInRealRegister + nominalIndex;
        return this.realRegister.sampleRealQubit(realIndex);
    }

    public Integer measureQubit(int nominalIndex) {
        if (nominalIndex < 0 || nominalIndex >= this.numQubits) {
            throw new IndexOutOfBoundsException("Индекс " + nominalIndex + " вне границ номинального регистра " + name + " размера " + numQubits);
        }
        int realIndex = this.offsetInRealRegister + nominalIndex;
        return this.realRegister.measureRealQubit(realIndex);
    }

    private Integer sampleRealQubit(int realIndex) {
        if (this.realRegister != this) {
            throw new IllegalStateException("sampleRealQubit можно вызывать только на реальном регистре");
        }
        if (realIndex < 0 || realIndex >= this.numQubits) {
            throw new IndexOutOfBoundsException("Индекс " + realIndex + " вне границ реального регистра " + name + " размера " + numQubits);
        }

        double zeroProb = 0;
        for (int i = this.states.nextSetBit(0); i >= 0; i = this.states.nextSetBit(i + 1)) {
            if (((i >> realIndex) & 1) == 0) {
                zeroProb += this.amplitudes.get(i).modulusSquared();
            }
        }

        if (this.random.nextDouble() <= zeroProb) {
            return 0;
        } else {
            return 1;
        }
    }

    private Integer measureRealQubit(int realIndex) {
        if (this.realRegister != this) {
            throw new IllegalStateException("measureRealQubit можно вызывать только на реальном регистре");
        }
        if (realIndex < 0 || realIndex >= this.numQubits) {
            throw new IndexOutOfBoundsException("Индекс " + realIndex + " вне границ реального регистра " + name + " размера " + numQubits);
        }

        double zeroProb = 0;
        for (int i = this.states.nextSetBit(0); i >= 0; i = this.states.nextSetBit(i + 1)) {
            if (((i >> realIndex) & 1) == 0) {
                zeroProb += this.amplitudes.get(i).modulusSquared();
            }
        }

        int measuredValue;
        if (this.random.nextDouble() <= zeroProb) {
            measuredValue = 0;
        } else {
            measuredValue = 1;
        }

        double normFactorSquared = 0;
        BitSet statesToRemove = new BitSet();
        for (int i = this.states.nextSetBit(0); i >= 0; i = this.states.nextSetBit(i + 1)) {
            if (((i >> realIndex) & 1) != measuredValue) {
                statesToRemove.set(i);
            } else {
                normFactorSquared += this.amplitudes.get(i).modulusSquared();
            }
        }

        for (int i = statesToRemove.nextSetBit(0); i >= 0; i = statesToRemove.nextSetBit(i + 1)) {
            this.states.clear(i);
            this.amplitudes.set(i, Complex.getZero());
        }

        if (normFactorSquared > 1e-12) {
            double normFactor = Math.sqrt(normFactorSquared);
            Complex normComplex = new Complex(normFactor, 0.0);
            for (int i = this.states.nextSetBit(0); i >= 0; i = this.states.nextSetBit(i + 1)) {
                Complex oldAmplitude = this.amplitudes.get(i);
                this.amplitudes.set(i, oldAmplitude.divide(normComplex));
            }
        } else {
            this.states.clear();
        }

        return measuredValue;
    }

    @Override
    public String toString() {
        QubitRegister regToPrint = this.realRegister;
        StringBuilder sb = new StringBuilder();
        BitSet currentStates = regToPrint.getStates();
        ChunkedComplexArray currentAmplitudes = regToPrint.getAmplitudes();
        int sizeToPrint = regToPrint.numQubits;

        for (int i = currentStates.nextSetBit(0); i >= 0; i = currentStates.nextSetBit(i + 1)) {
            Complex amplitude = currentAmplitudes.get(i);
            if (amplitude != null && !amplitude.equals(Complex.getZero())) {
                String binaryState = String.format("%" + sizeToPrint + "s",
                        Integer.toBinaryString(i)).replace(' ', '0');
                sb.append("|").append(binaryState).append(">: ")
                        .append(amplitude).append("\n");
            }
        }
        if (sb.length() == 0) {
            String binaryState = String.format("%" + sizeToPrint + "s", 0).replace(' ', '0');
            sb.append("|").append(binaryState).append(">: ")
                    .append(Complex.getZero()).append("\n");
        }
        return sb.toString();
    }

    // Статический метод для вычисления тензорного произведения двух реальных регистров
    public static QubitRegister tensorProduct(QubitRegister reg1, QubitRegister reg2, String newRealName) {
        if (reg1.getRealRegister() != reg1 || reg2.getRealRegister() != reg2) {
            throw new IllegalArgumentException("Тензорное произведение можно вычислять только для реальных регистров.");
        }

        int size1 = reg1.size();
        int size2 = reg2.size();
        int joinedSize = size1 + size2;

        QubitRegister joinedRegister = new QubitRegister(newRealName, joinedSize);

        BitSet states1 = reg1.getStates();
        ChunkedComplexArray amps1 = reg1.getAmplitudes();
        BitSet states2 = reg2.getStates();
        ChunkedComplexArray amps2 = reg2.getAmplitudes();

        BitSet joinedStates = joinedRegister.getStates();
        ChunkedComplexArray joinedAmps = joinedRegister.getAmplitudes();
        joinedStates.clear();
        joinedAmps.set(0, Complex.getZero());

        int factor = (1 << size2);
        for (int s1 = states1.nextSetBit(0); s1 >= 0; s1 = states1.nextSetBit(s1 + 1)) {
            Complex amp1 = amps1.get(s1);
            if (amp1 == null || amp1.equals(Complex.getZero())) continue;

            for (int s2 = states2.nextSetBit(0); s2 >= 0; s2 = states2.nextSetBit(s2 + 1)) {
                Complex amp2 = amps2.get(s2);
                if (amp2 == null || amp2.equals(Complex.getZero())) continue;

                int joinedIndex = s1 * factor + s2;
                Complex joinedAmplitude = amp1.multiply(amp2);

                if (!joinedAmplitude.equals(Complex.getZero())) {
                    joinedStates.set(joinedIndex);
                    joinedAmps.set(joinedIndex, joinedAmplitude);
                }
            }
        }
        System.out.println("Вычислено тензорное произведение для " + newRealName);
        return joinedRegister;
    }
}
