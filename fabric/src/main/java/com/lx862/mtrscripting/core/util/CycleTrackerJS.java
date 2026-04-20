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

@SuppressWarnings("unused")
public class CycleTrackerJS {
    private final String[] states;
    private final float[] offsets;
    private final float cycleDuration;

    public CycleTrackerJS(Object[] params) {
        if (params.length % 2 != 0) throw new IllegalArgumentException();
        float offset = 0;
        states = new String[params.length / 2];
        offsets = new float[params.length / 2];
        for (int i = 0; i < params.length; i += 2) {
            states[i / 2] = params[i].toString();
            float elemDuration = Float.parseFloat(params[i + 1].toString());
            offsets[i / 2] = offset;
            offset += elemDuration;
        }
        cycleDuration = offset;
    }

    private String lastState;
    private String currentState;
    private double currentStateTime;
    private int lastStateNum;
    private boolean firstTimeCurrentState;

    public void tick() {
        double time = TimingJS.globalElapsed() % cycleDuration;
        int cycleNum = (int) (TimingJS.globalElapsed() / cycleDuration);
        for (int i = offsets.length - 1; i >= 0; i--) {
            if (time >= offsets[i]) {
                int stateNum = cycleNum * offsets.length + i;
                currentState = states[i];
                currentStateTime = cycleNum * cycleDuration + offsets[i];
                lastState = states[i == 0 ? offsets.length - 1 : i - 1];
                if (lastStateNum != stateNum) {
                    firstTimeCurrentState = true;
                    lastStateNum = stateNum;
                } else {
                    firstTimeCurrentState = false;
                }
                break;
            }
        }
    }

    public String stateNow() {
        return currentState;
    }

    public String stateLast() {
        return lastState;
    }

    public double stateNowDuration() {
        return TimingJS.globalElapsed() - currentStateTime;
    }

    public boolean stateNowFirst() {
        return firstTimeCurrentState;
    }
}