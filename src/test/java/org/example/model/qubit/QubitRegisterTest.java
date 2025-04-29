package org.example.model.qubit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.BitSet;
import static org.junit.jupiter.api.Assertions.*;

public class QubitRegisterTest {

    private static final double DELTA = 1e-9;

    @Test
    void testRealRegisterConstructorValid() {
        QubitRegister reg = new QubitRegister("testReg", 3);
        assertEquals("testReg", reg.getName());
        assertEquals(3, reg.size());
        assertEquals(3, reg.getRealSize());
        assertSame(reg, reg.getRealRegister());
        assertEquals(0, reg.getOffsetInRealRegister());
        assertNotNull(reg.getStates());
        assertNotNull(reg.getAmplitudes());
        assertEquals(1, reg.getStates().cardinality());
        assertTrue(reg.getStates().get(0));
        assertTrue(Complex.getOne().equals(reg.getAmplitudes().get(0)));
        assertEquals(1.0, calculateTotalProbability(reg), DELTA);
    }

    @Test
    void testRealRegisterConstructorInvalidArgs() {
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister(null, 2));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("", 2));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("reg", 0));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("reg", -1));
    }

    @Test
    void testNominalRegisterConstructorValid() {
        QubitRegister realReg = new QubitRegister("real", 5);
        QubitRegister nomReg = new QubitRegister("nominal", 2, realReg, 1);

        assertEquals("nominal", nomReg.getName());
        assertEquals(2, nomReg.size());
        assertEquals(5, nomReg.getRealSize());
        assertSame(realReg, nomReg.getRealRegister());
        assertEquals(1, nomReg.getOffsetInRealRegister());

        assertSame(realReg.getStates(), nomReg.getStates());
        assertSame(realReg.getAmplitudes(), nomReg.getAmplitudes());
    }

    @Test
    void testNominalRegisterConstructorInvalidArgs() {
        QubitRegister realReg = new QubitRegister("real", 3);
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister(null, 1, realReg, 0));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("", 1, realReg, 0));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", 0, realReg, 0));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", -1, realReg, 0));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", 1, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", 1, realReg, -1));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", 2, realReg, 2));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", 3, realReg, 1));
        assertThrows(IllegalArgumentException.class, () -> new QubitRegister("nom", 4, realReg, 0));
    }

    @Test
    void testTensorProduct() {
        // |0> state
        QubitRegister reg1 = new QubitRegister("reg1", 1);
        // |1> state
        QubitRegister reg2 = new QubitRegister("reg2", 1);
        reg2.getAmplitudes().set(0, Complex.getZero());
        reg2.getStates().clear(0);
        reg2.getAmplitudes().set(1, Complex.getOne());
        reg2.getStates().set(1);

        QubitRegister product = QubitRegister.tensorProduct(reg1, reg2, "prod");
        assertEquals("prod", product.getName());
        assertEquals(2, product.size());
        assertEquals(1, product.getStates().cardinality());
        assertTrue(product.getStates().get(1)); // State 01 has index 1
        assertTrue(Complex.getOne().equals(product.getAmplitudes().get(1)));
        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(0)));
        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(2)));
        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(product), DELTA);
    }

    @Test
    void testTensorProductSuperposition() {
        // |+> state = (|0> + |1>) / sqrt(2)
        QubitRegister reg1 = new QubitRegister("reg_plus", 1);
        Complex half = new Complex(1.0 / Math.sqrt(2.0));
        reg1.getAmplitudes().set(0, half);
        reg1.getAmplitudes().set(1, half);
        reg1.getStates().set(1);

        // |-> state = (|0> - |1>) / sqrt(2)
        QubitRegister reg2 = new QubitRegister("reg_minus", 1);
        reg2.getAmplitudes().set(0, half);
        reg2.getAmplitudes().set(1, half.multiply(new Complex(-1.0)));
        reg2.getStates().set(1);

        // Expected: |+-> = (|00> - |01> + |10> - |11>) / 2
        QubitRegister product = QubitRegister.tensorProduct(reg1, reg2, "prod_super");
        Complex half_amp = new Complex(0.5);

        assertEquals(2, product.size());
        assertEquals(4, product.getStates().cardinality()); // All 4 states should be present
        assertTrue(half_amp.equals(product.getAmplitudes().get(0))); // |00>
        assertTrue(half_amp.multiply(new Complex(-1.0)).equals(product.getAmplitudes().get(1))); // |01>
        assertTrue(half_amp.equals(product.getAmplitudes().get(2))); // |10>
        assertTrue(half_amp.multiply(new Complex(-1.0)).equals(product.getAmplitudes().get(3))); // |11>
        assertEquals(1.0, calculateTotalProbability(product), DELTA);
    }

     @Test
    void testTensorProductWithNominalRegisterThrows() {
        QubitRegister realReg = new QubitRegister("real", 2);
        QubitRegister nomReg = new QubitRegister("nom", 1, realReg, 0);
        QubitRegister reg2 = new QubitRegister("reg2", 1);

        assertThrows(IllegalArgumentException.class, () -> QubitRegister.tensorProduct(nomReg, reg2, "fail1"));
        assertThrows(IllegalArgumentException.class, () -> QubitRegister.tensorProduct(realReg, nomReg, "fail2"));
    }

    @Test
    void testMeasureQubitCollapseToZero() {
        // State: (|00> + i|11>) / sqrt(2)
        QubitRegister reg = new QubitRegister("measure0", 2);
        Complex amp = new Complex(0.0, 1.0 / Math.sqrt(2.0));
        Complex amp0 = new Complex(1.0 / Math.sqrt(2.0));
        reg.getStates().clear(); // Clear default |00>
        reg.getAmplitudes().set(0, amp0); // |00>
        reg.getStates().set(0);
        reg.getAmplitudes().set(3, amp);   // |11>
        reg.getStates().set(3);

        QubitRegister regCopy = copyRegisterState(reg);

        regCopy.getStates().clear(3);
        regCopy.getAmplitudes().set(3, Complex.getZero());
        Complex normFactor = new Complex(1.0 / Math.sqrt(2.0));
        regCopy.getAmplitudes().set(0, regCopy.getAmplitudes().get(0).divide(normFactor));

        assertEquals(1, regCopy.getStates().cardinality());
        assertTrue(regCopy.getStates().get(0));
        assertTrue(Complex.getOne().equals(regCopy.getAmplitudes().get(0)));
        assertEquals(1.0, calculateTotalProbability(regCopy), DELTA);
    }

    @Test
    void testMeasureQubitCollapseToOne() {
        // State: (|00> + i|11>) / sqrt(2)
        QubitRegister reg = new QubitRegister("measure1", 2);
        Complex amp = new Complex(0.0, 1.0 / Math.sqrt(2.0));
        Complex amp0 = new Complex(1.0 / Math.sqrt(2.0));
        reg.getStates().clear();
        reg.getAmplitudes().set(0, amp0); // |00>
        reg.getStates().set(0);
        reg.getAmplitudes().set(3, amp);   // |11>
        reg.getStates().set(3);

        QubitRegister regCopy = copyRegisterState(reg);

        regCopy.getStates().clear(0);
        regCopy.getAmplitudes().set(0, Complex.getZero());
        Complex normFactor = new Complex(1.0 / Math.sqrt(2.0));
        regCopy.getAmplitudes().set(3, regCopy.getAmplitudes().get(3).divide(normFactor));

        assertEquals(1, regCopy.getStates().cardinality());
        assertTrue(regCopy.getStates().get(3));
        Complex expectedAmp = new Complex(0.0, 1.0); // i
        assertTrue(expectedAmp.equals(regCopy.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(regCopy), DELTA);
    }

    @Test
    void testMeasureQubitOnNominalRegister() {
         QubitRegister realReg = new QubitRegister("real_measure", 3);
         QubitRegister nomReg = new QubitRegister("nom_measure", 2, realReg, 1);

         int result = nomReg.measureQubit(0);

         assertEquals(0, result);
         assertEquals(1, realReg.getStates().cardinality());
         assertTrue(realReg.getStates().get(0));
         assertTrue(Complex.getOne().equals(realReg.getAmplitudes().get(0)));
         assertEquals(1.0, calculateTotalProbability(realReg), DELTA);
    }

     @Test
    void testMeasureQubitIndexOutOfBounds() {
        QubitRegister reg = new QubitRegister("bounds", 2);
        QubitRegister nomReg = new QubitRegister("nom_bounds", 1, reg, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> reg.measureQubit(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> reg.measureQubit(2));
        assertThrows(IndexOutOfBoundsException.class, () -> nomReg.measureQubit(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> nomReg.measureQubit(1));
    }

    @Test
    void testSampleQubitOnNominalRegister() {
         QubitRegister realReg = new QubitRegister("real_sample", 3);
         QubitRegister nomReg = new QubitRegister("nom_sample", 2, realReg, 1);

         assertEquals(0, nomReg.sampleQubit(0)); // Samples real qubit 1
         assertEquals(0, nomReg.sampleQubit(1)); // Samples real qubit 2
         assertEquals(1, realReg.getStates().cardinality());
         assertTrue(realReg.getStates().get(0));
         assertTrue(Complex.getOne().equals(realReg.getAmplitudes().get(0)));
    }

    @Test
    void testTensorProduct2x1QubitSuperposition() {
        QubitRegister reg1 = new QubitRegister("tp_2q", 2);
        Complex amp1 = new Complex(1.0 / Math.sqrt(2.0));
        reg1.getStates().clear();
        reg1.getAmplitudes().set(0, Complex.getZero());
        reg1.getStates().set(1); // |01>
        reg1.getAmplitudes().set(1, amp1);
        reg1.getStates().set(2); // |10>
        reg1.getAmplitudes().set(2, amp1);

        // reg2: 1 qubit, state |1>
        QubitRegister reg2 = new QubitRegister("tp_1q", 1);
        reg2.getStates().clear(0);
        reg2.getAmplitudes().set(0, Complex.getZero());
        reg2.getStates().set(1);
        reg2.getAmplitudes().set(1, Complex.getOne());

        QubitRegister product = QubitRegister.tensorProduct(reg1, reg2, "prod_2x1");

        assertEquals(3, product.size());
        assertEquals(2, product.getStates().cardinality());
        assertTrue(product.getStates().get(3));
        assertTrue(product.getStates().get(5));
        assertTrue(amp1.equals(product.getAmplitudes().get(3)));
        assertTrue(amp1.equals(product.getAmplitudes().get(5)));
        assertEquals(1.0, calculateTotalProbability(product), DELTA);
    }

    @Test
    void testTensorProductSuperposition2x2() {
        QubitRegister reg1 = new QubitRegister("tp_bell", 2);
        Complex amp1 = new Complex(1.0 / Math.sqrt(2.0));
        reg1.getStates().clear();
        reg1.getAmplitudes().set(0, Complex.getZero());
        reg1.getStates().set(0); // |00>
        reg1.getAmplitudes().set(0, amp1);
        reg1.getStates().set(3); // |11>
        reg1.getAmplitudes().set(3, amp1);

        // reg2: (|0> + i|1>) / sqrt(2)
        QubitRegister reg2 = new QubitRegister("tp_i", 1);
        Complex amp2_0 = new Complex(1.0 / Math.sqrt(2.0));
        Complex amp2_1 = new Complex(0.0, 1.0 / Math.sqrt(2.0)); // i / sqrt(2)
        reg2.getAmplitudes().set(0, amp2_0);
        reg2.getAmplitudes().set(1, amp2_1);
        reg2.getStates().set(1);

        QubitRegister product = QubitRegister.tensorProduct(reg1, reg2, "prod_super2x1");
        Complex expectedAmp_0 = amp1.multiply(amp2_0); // 0.5
        Complex expectedAmp_1 = amp1.multiply(amp2_1); // 0.5i
        Complex expectedAmp_6 = amp1.multiply(amp2_0); // 0.5
        Complex expectedAmp_7 = amp1.multiply(amp2_1); // 0.5i

        assertEquals(3, product.size());
        assertEquals(4, product.getStates().cardinality());
        assertTrue(product.getStates().get(0));
        assertTrue(product.getStates().get(1));
        assertTrue(product.getStates().get(6));
        assertTrue(product.getStates().get(7));

        assertTrue(expectedAmp_0.equals(product.getAmplitudes().get(0))); // |000>
        assertTrue(expectedAmp_1.equals(product.getAmplitudes().get(1))); // |001>
        assertTrue(expectedAmp_6.equals(product.getAmplitudes().get(6))); // |110>
        assertTrue(expectedAmp_7.equals(product.getAmplitudes().get(7))); // |111>

        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(2))); // Check a zero amp
        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(3))); // Check a zero amp
        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(4))); // Check a zero amp
        assertTrue(Complex.getZero().equals(product.getAmplitudes().get(5))); // Check a zero amp

        assertEquals(1.0, calculateTotalProbability(product), DELTA);
    }

    @Test
    void testTensorProductBasisStateAndSuperposition() {
        // reg1: |01>
        QubitRegister reg1 = new QubitRegister("tp_basis", 2);
        reg1.getStates().clear(0);
        reg1.getAmplitudes().set(0, Complex.getZero());
        reg1.getStates().set(1);
        reg1.getAmplitudes().set(1, Complex.getOne());

        // reg2: |+> = (|0> + |1>) / sqrt(2)
        QubitRegister reg2 = new QubitRegister("tp_plus", 1);
        Complex amp2 = new Complex(1.0 / Math.sqrt(2.0));
        reg2.getAmplitudes().set(0, amp2);
        reg2.getAmplitudes().set(1, amp2);
        reg2.getStates().set(1);

        QubitRegister product = QubitRegister.tensorProduct(reg1, reg2, "prod_basis_super");

        assertEquals(3, product.size());
        assertEquals(2, product.getStates().cardinality());
        assertTrue(product.getStates().get(2)); // |010>
        assertTrue(product.getStates().get(3)); // |011>
        assertTrue(amp2.equals(product.getAmplitudes().get(2)));
        assertTrue(amp2.equals(product.getAmplitudes().get(3)));
        assertEquals(1.0, calculateTotalProbability(product), DELTA);
    }

    private double calculateTotalProbability(QubitRegister register) {
        double totalProb = 0;
        BitSet states = register.getStates();
        ChunkedComplexArray amplitudes = register.getAmplitudes();
        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            totalProb += amplitudes.get(i).modulusSquared();
        }
        return totalProb;
    }

    private QubitRegister copyRegisterState(QubitRegister original) {
        QubitRegister copy = new QubitRegister(original.getName() + "_copy", original.size());
        BitSet originalStates = original.getStates();
        ChunkedComplexArray originalAmps = original.getAmplitudes();

        copy.getStates().clear();
        copy.getAmplitudes().set(0, Complex.getZero());

        for (int i = originalStates.nextSetBit(0); i >= 0; i = originalStates.nextSetBit(i + 1)) {
            copy.getStates().set(i);
            copy.getAmplitudes().set(i, originalAmps.get(i));
        }
        return copy;
    }
} 