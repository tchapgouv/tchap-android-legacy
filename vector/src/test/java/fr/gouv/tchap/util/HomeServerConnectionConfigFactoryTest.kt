/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package fr.gouv.tchap.util

import junit.framework.Assert.assertEquals
import org.junit.Test

class HomeServerConnectionConfigFactoryTest {

    @Test
    fun createHomeServerConnectionConfig() {
    }

    @Test
    fun getBytesFromString_ok() {
        val res = getBytesFromString("07:89:7A:A0:30:82:99:95:E6:17:5D:1F:34:5D:8D:0C:67:82:63:1C:1F:57:20:75:42:91:F7:8B:28:03:54:A2")

        assertEquals(32, res.size)

        assertEquals(0x07, res[0].toPositiveInt())
        assertEquals(0x89, res[1].toPositiveInt())
        assertEquals(0x7A, res[2].toPositiveInt())
        assertEquals(0xA0, res[3].toPositiveInt())
        assertEquals(0x30, res[4].toPositiveInt())
        assertEquals(0x82, res[5].toPositiveInt())
        assertEquals(0x99, res[6].toPositiveInt())
        assertEquals(0x95, res[7].toPositiveInt())
        assertEquals(0xE6, res[8].toPositiveInt())
        assertEquals(0x17, res[9].toPositiveInt())
        assertEquals(0x5D, res[10].toPositiveInt())
        assertEquals(0x1F, res[11].toPositiveInt())
        assertEquals(0x34, res[12].toPositiveInt())
        assertEquals(0x5D, res[13].toPositiveInt())
        assertEquals(0x8D, res[14].toPositiveInt())
        assertEquals(0x0C, res[15].toPositiveInt())
        assertEquals(0x67, res[16].toPositiveInt())
        assertEquals(0x82, res[17].toPositiveInt())
        assertEquals(0x63, res[18].toPositiveInt())
        assertEquals(0x1C, res[19].toPositiveInt())
        assertEquals(0x1F, res[20].toPositiveInt())
        assertEquals(0x57, res[21].toPositiveInt())
        assertEquals(0x20, res[22].toPositiveInt())
        assertEquals(0x75, res[23].toPositiveInt())
        assertEquals(0x42, res[24].toPositiveInt())
        assertEquals(0x91, res[25].toPositiveInt())
        assertEquals(0xF7, res[26].toPositiveInt())
        assertEquals(0x8B, res[27].toPositiveInt())
        assertEquals(0x28, res[28].toPositiveInt())
        assertEquals(0x03, res[29].toPositiveInt())
        assertEquals(0x54, res[30].toPositiveInt())
        assertEquals(0xA2, res[31].toPositiveInt())
    }
}

private fun Byte.toPositiveInt() = toInt() and 0xFF