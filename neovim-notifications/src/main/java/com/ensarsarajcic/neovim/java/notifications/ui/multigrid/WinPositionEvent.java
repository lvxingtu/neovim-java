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

package com.ensarsarajcic.neovim.java.notifications.ui.multigrid;

import com.ensarsarajcic.neovim.java.api.types.msgpack.Window;
import com.ensarsarajcic.neovim.java.notifications.ui.grid.UIGridEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public final class WinPositionEvent implements UIMultigridEvent {
    public static final String NAME = "win_pos";

    private int grid;
    private Window win;
    private int startRow;
    private int startCol;
    private int width;
    private int height;

    public WinPositionEvent(
            @JsonProperty(value = "grid", index = 0) int grid,
            @JsonProperty(value = "win", index = 1) Window win,
            @JsonProperty(value = "start_row", index = 2) int startRow,
            @JsonProperty(value = "start_col", index = 3) int startCol,
            @JsonProperty(value = "width", index = 4) int width,
            @JsonProperty(value = "height", index = 5) int height) {
        this.grid = grid;
        this.win = win;
        this.startRow = startRow;
        this.startCol = startCol;
        this.width = width;
        this.height = height;
    }

    public int getGrid() {
        return grid;
    }

    public Window getWin() {
        return win;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String getEventName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "WinPositionEvent{" +
                "grid=" + grid +
                ", win=" + win +
                ", startRow=" + startRow +
                ", startCol=" + startCol +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
