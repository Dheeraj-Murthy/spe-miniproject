package com.dheeraj.miniproject.service;

import com.dheeraj.miniproject.exception.DivisionByZeroException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private final CalculatorService calculatorService = new CalculatorService();

    @Test
    void add_returnsSum() {
        assertEquals(5, calculatorService.add(2, 3));
    }

    @Test
    void subtract_returnsDifference() {
        assertEquals(-1, calculatorService.subtract(2, 3));
    }

    @Test
    void multiply_returnsProduct() {
        assertEquals(6, calculatorService.multiply(2, 3));
    }

    @Test
    void divide_returnsQuotient() {
        assertEquals(2, calculatorService.divide(6, 3));
    }

    @Test
    void divide_byZero_throwsException() {
        assertThrows(DivisionByZeroException.class, () -> calculatorService.divide(5, 0));
    }
}
