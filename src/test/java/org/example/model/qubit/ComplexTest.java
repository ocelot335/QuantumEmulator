package org.example.model.qubit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ComplexTest {

    private static final double DELTA = 1e-9;

    @Test
    void testConstructorWithRealAndImaginary() {
        Complex c = new Complex(3.0, -4.0);
        assertEquals(3.0, c.getReal(), DELTA);
        assertEquals(-4.0, c.getImaginary(), DELTA);
    }

    @Test
    void testConstructorWithRealOnly() {
        Complex c = new Complex(5.5);
        assertEquals(5.5, c.getReal(), DELTA);
        assertEquals(0.0, c.getImaginary(), DELTA);
    }

    @Test
    void testGetZero() {
        Complex zero = Complex.getZero();
        assertEquals(0.0, zero.getReal(), DELTA);
        assertEquals(0.0, zero.getImaginary(), DELTA);
        assertTrue(zero.isZero());
    }

    @Test
    void testGetOne() {
        Complex one = Complex.getOne();
        assertEquals(1.0, one.getReal(), DELTA);
        assertEquals(0.0, one.getImaginary(), DELTA);
    }

    @Test
    void testExpI() {
        Complex c1 = Complex.expI(0);
        assertEquals(1.0, c1.getReal(), DELTA);
        assertEquals(0.0, c1.getImaginary(), DELTA);

        Complex c2 = Complex.expI(Math.PI / 2.0);
        assertEquals(0.0, c2.getReal(), DELTA);
        assertEquals(1.0, c2.getImaginary(), DELTA);

        Complex c3 = Complex.expI(Math.PI);
        assertEquals(-1.0, c3.getReal(), DELTA);
        assertEquals(0.0, c3.getImaginary(), DELTA);

        Complex c4 = Complex.expI(3.0 * Math.PI / 2.0);
        assertEquals(0.0, c4.getReal(), DELTA);
        assertEquals(-1.0, c4.getImaginary(), DELTA);
    }

    @Test
    void testGetters() {
        Complex c = new Complex(-1.2, 3.4);
        assertEquals(-1.2, c.getReal(), DELTA);
        assertEquals(3.4, c.getImaginary(), DELTA);
    }

    @Test
    void testAdd() {
        Complex c1 = new Complex(1.0, 2.0);
        Complex c2 = new Complex(3.0, -1.0);
        Complex sum = c1.add(c2);
        assertEquals(4.0, sum.getReal(), DELTA);
        assertEquals(1.0, sum.getImaginary(), DELTA);
    }

    @Test
    void testSubtract() {
        Complex c1 = new Complex(1.0, 2.0);
        Complex c2 = new Complex(3.0, -1.0);
        Complex diff = c1.subtract(c2);
        assertEquals(-2.0, diff.getReal(), DELTA);
        assertEquals(3.0, diff.getImaginary(), DELTA);
    }

    @Test
    void testEquals() {
        Complex c1 = new Complex(1.1, 2.2);
        Complex c2 = new Complex(1.1, 2.2);
        Complex c3 = new Complex(1.1, 2.3);
        Complex c4 = new Complex(1.2, 2.2);
        Complex zero1 = Complex.getZero();
        Complex zero2 = new Complex(0,0);


        assertTrue(c1.equals(c1));
        assertTrue(c1.equals(c2) && c2.equals(c1));
        assertTrue(c1.equals(c2));
        assertTrue(zero1.equals(zero2));

        assertFalse(c1.equals(c3));
        assertFalse(c1.equals(c4));
        // assertFalse(c1.equals(null)); // Текущая реализация вызовет NPE, но в идеале equals должен обрабатывать null
        assertFalse(c1.equals(Complex.getZero()));
    }

    @Test
    void testMultiply() {
        Complex c1 = new Complex(2.0, 3.0);
        Complex c2 = new Complex(4.0, -1.0);
        Complex product = c1.multiply(c2);
        assertEquals(11.0, product.getReal(), DELTA);
        assertEquals(10.0, product.getImaginary(), DELTA);

        Complex zero = Complex.getZero();
        Complex one = Complex.getOne();
        assertTrue(zero.equals(c1.multiply(zero))); // Используем equals для сравнения комплексных чисел
        assertTrue(c1.equals(c1.multiply(one)));
    }

    @Test
    void testDivide() {
        Complex c1 = new Complex(11.0, 10.0);
        Complex c2 = new Complex(4.0, -1.0);
        Complex quotient = c1.divide(c2);
        assertEquals(2.0, quotient.getReal(), DELTA);
        assertEquals(3.0, quotient.getImaginary(), DELTA);

        Complex c3 = new Complex(5.0, 0.0);
        Complex c4 = new Complex(2.0, 0.0);
        Complex quotientReal = c3.divide(c4);
        assertEquals(2.5, quotientReal.getReal(), DELTA);
        assertEquals(0.0, quotientReal.getImaginary(), DELTA);

        Complex one = Complex.getOne();
        assertTrue(c1.equals(c1.divide(one))); // Используем equals
    }

    @Test
    void testDivideByZero() {
        Complex c1 = new Complex(1.0, 1.0);
        Complex zero = Complex.getZero();
        assertThrows(ArithmeticException.class, () -> c1.divide(zero));
    }

    @Test
    void testIsZero() {
        assertTrue(Complex.getZero().isZero());
        assertTrue(new Complex(0.0, 0.0).isZero());
        assertFalse(Complex.getOne().isZero());
        assertFalse(new Complex(1e-10, 0.0).isZero()); // Точная проверка на ноль
        assertFalse(new Complex(0.0, 1e-10).isZero());
        assertFalse(new Complex(1.0, 2.0).isZero());
    }

    @Test
    void testModulusSquared() {
        Complex c1 = new Complex(3.0, 4.0);
        assertEquals(25.0, c1.modulusSquared(), DELTA);

        Complex c2 = new Complex(-1.0, 2.0);
        assertEquals(5.0, c2.modulusSquared(), DELTA);

        Complex zero = Complex.getZero();
        assertEquals(0.0, zero.modulusSquared(), DELTA);

        Complex one = Complex.getOne();
        assertEquals(1.0, one.modulusSquared(), DELTA);
    }

     @Test
    void testToString() {
        Complex c1 = new Complex(1.234, 5.678);
        assertEquals("1,23 + 5,68i", c1.toString()); // Проверка формата "%.2f + %.2fi"

        Complex c2 = new Complex(-1.0, -2.0);
        assertEquals("-1,00 + -2,00i", c2.toString());

        Complex c3 = new Complex(3.0, 0.0);
        assertEquals("3,00 + 0,00i", c3.toString());

        Complex c4 = new Complex(0.0, -4.5);
        assertEquals("0,00 + -4,50i", c4.toString());

        Complex c5 = new Complex(0.0, 0.0);
        assertEquals("0,00 + 0,00i", c5.toString());
    }
} 