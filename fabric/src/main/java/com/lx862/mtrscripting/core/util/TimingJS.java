/**
 * MIT License
 *
 * Copyright (c) 2022-present Zbx1425
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

package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.mapping.holder.MinecraftClient;

@SuppressWarnings("unused")
public class TimingJS {
    private static double totalRunningTime = 0;
    private double timeElapsedForScript = 0;
    private double frameDeltaForScript = 0;

    @ApiInternal
    public static void update(double elapsed) {
        totalRunningTime += MinecraftClient.getInstance().isPaused() ? 0 : elapsed / 1000.0;
    }

    @ApiInternal
    public void prepareForScript(ScriptInstance<?> scriptInstance) {
        timeElapsedForScript = totalRunningTime;
        frameDeltaForScript = totalRunningTime - scriptInstance.lastExecuteTime;
        scriptInstance.lastExecuteTime = timeElapsedForScript;
    }

    public static double globalElapsed() {
        return totalRunningTime;
    }

    public double elapsed() {
        return timeElapsedForScript;
    }

    public double delta() {
        return frameDeltaForScript;
    }

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long nanoTime() {
        return System.nanoTime();
    }
}