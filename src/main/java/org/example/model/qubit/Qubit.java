package org.example.model.qubit;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Map;
import java.util.Random;

public class Qubit implements Serializable {
    //private Complex alpha; // Амплитуда для |0>
    //private Complex beta;  // Амплитуда для |1>
    private String registerName;
    private int registerId;
    private QubitRegister qubitRegister;

    // Конструктор по умолчанию создает кубит в состоянии |0>
    public Qubit(QubitRegister qubitRegister, String registerName, int registerId) {
        this.registerName = registerName;
        this.registerId = registerId;
        this.qubitRegister = qubitRegister;
    }


    public int sample() {
        double zeroProb = 0;
        BitSet bs = qubitRegister.getStates();
        Complex[] ampls = qubitRegister.getAmplitudes();
        for (int i = bs.nextSetBit(0); i >= 0 ; i = bs.nextSetBit(i+1)) {
            if((i>>registerId)%2==0) {
                zeroProb += ampls[i].modulusSquared();
            }
        }

        Random random = new Random();
        if(random.nextDouble()<=zeroProb) {
            return 0;
        } else {
            return 1;
        }
    }


    private boolean isNormalized(Complex alpha, Complex beta) {
        double norm = alpha.modulusSquared() + beta.modulusSquared();
        return Math.abs(norm - 1.0) < 1e-8; // Учет погрешности
    }
}
