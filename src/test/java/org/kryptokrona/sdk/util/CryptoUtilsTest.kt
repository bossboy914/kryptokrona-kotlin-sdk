package org.kryptokrona.sdk.util;

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CryptoUtilsTest {

    @Test
    fun `can enter valid mnemonic word`() {
        assertTrue(CryptoUtils.isValidMnemonicWord("adapt"))
    }

    @Test
    fun `can not enter invalid mnemonic word`() {
        assertFalse(CryptoUtils.isValidMnemonicWord("mjovanc"))
    }

    @Test
    fun `input is unlocked`() {
        assertTrue(CryptoUtils.isInputUnlocked(0, 1000))
    }

    @Test
    fun `input is locked`() {
        assertTrue(CryptoUtils.isInputUnlocked(100, 1000))
    }

    @Test
    fun `can pretty print an amount`() {
        val prettyPrintedAmount1 = CryptoUtils.prettyPrintAmount(12345.01)
        val prettyPrintedAmount2 = CryptoUtils.prettyPrintAmount(123456.01)
        val prettyPrintedAmount3 = CryptoUtils.prettyPrintAmount(1234567.01)
        val prettyPrintedAmount4 = CryptoUtils.prettyPrintAmount(12345678.01)
        val prettyPrintedAmount5 = CryptoUtils.prettyPrintAmount(123456789.01)

        assertEquals("XKR 12,345.01", prettyPrintedAmount1)
        assertEquals("XKR 123,456.01", prettyPrintedAmount2)
        assertEquals("XKR 1,234,567.01", prettyPrintedAmount3)
        assertEquals("XKR 12,345,678.01", prettyPrintedAmount4)
        assertEquals("XKR 123,456,789.01", prettyPrintedAmount5)
    }
}