/*
 * MIT License
 *
 * Copyright (c) 2018 Ensar Sarajčić
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ensarsarajcic.neovim.java.api.types.msgpack;

import org.junit.Test;

import static org.junit.Assert.*;

public class BufferTest {

    @Test
    public void equalsTest() {
        Buffer bufferOne = new Buffer(1);
        Buffer bufferTwo = new Buffer(2);

        assertNotEquals(bufferOne, bufferTwo);
        assertEquals(bufferOne.equals(bufferTwo), bufferTwo.equals(bufferOne));

        Buffer bufferOneAgain = new Buffer(1);
        Buffer bufferOneSame = bufferOne;

        assertEquals(bufferOne, bufferOneSame);
        assertEquals(bufferOne, bufferOneAgain);
        assertEquals(bufferOne.equals(bufferOneAgain), bufferOneAgain.equals(bufferOneSame));
    }

    @Test
    public void hashCodeTest() {
        Buffer bufferOne = new Buffer(1);
        Buffer bufferTwo = new Buffer(2);

        assertNotEquals(bufferOne.hashCode(), bufferTwo.hashCode());

        Buffer bufferOneAgain = new Buffer(1);
        Buffer bufferOneSame = bufferOne;

        assertEquals(bufferOne.hashCode(), bufferOneSame.hashCode());
        assertEquals(bufferOne.hashCode(), bufferOneAgain.hashCode());
    }

    @Test
    public void compareToTest() {
        Buffer bufferOne = new Buffer(1);
        Buffer bufferTwo = new Buffer(2);

        assertTrue(bufferOne.compareTo(bufferTwo) < 0);
        assertTrue(bufferTwo.compareTo(bufferOne) > 0);

        Buffer bufferOneAgain = new Buffer(1);

        assertEquals(0, bufferOne.compareTo(bufferOneAgain));
        assertEquals(0, bufferOneAgain.compareTo(bufferOne));
    }
}