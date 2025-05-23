package org.example.model.qubit;

import java.io.Serializable;

public class Complex implements Serializable {
    private final double real;
    private final double imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public Complex(double real) {
        this.real = real;
        this.imaginary = 0;
    }

    public static Complex getZero() {
        return new Complex(0);
    }

    public static Complex getOne() {
        return new Complex(1);
    }

    //exp^(i*phase)
    public static Complex expI(double phase) {
        return new Complex(Math.cos(phase), Math.sin(phase));
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imaginary;
    }

    public Complex add(Complex other) {
        return new Complex(this.real + other.real, this.imaginary + other.imaginary);
    }

    public Complex subtract(Complex other) {
        return new Complex(this.real - other.real, this.imaginary - other.imaginary);
    }

    public boolean equals(Complex other) {
        return Math.abs(real-other.real)< 1e-9 && Math.abs(imaginary-other.imaginary)< 1e-9;
    }

    public Complex multiply(Complex other) {
        double realPart = this.real * other.real - this.imaginary * other.imaginary;
        double imaginaryPart = this.real * other.imaginary + this.imaginary * other.real;
        return new Complex(realPart, imaginaryPart);
    }

    public Complex divide(Complex other) {
        double denominator = other.real * other.real + other.imaginary * other.imaginary;
        if (denominator == 0) {
            throw new ArithmeticException("Division by zero in complex number division.");
        }
        double realPart = (this.real * other.real + this.imaginary * other.imaginary) / denominator;
        double imaginaryPart = (this.imaginary * other.real - this.real * other.imaginary) / denominator;
        return new Complex(realPart, imaginaryPart);
    }

    public boolean isZero() {
        return this.real == 0.0 && this.imaginary == 0.0;
    }

    public double modulusSquared() {
        return real * real + imaginary * imaginary;
    }

    @Override
    public String toString() {
        return String.format("%.2f + %.2fi", real, imaginary);
    }
}
