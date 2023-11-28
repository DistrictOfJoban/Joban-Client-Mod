package com.lx862.jcm.mod.render.fundamental;

public class Rectangle {
    public int x;
    public int y;
    public int width;
    public int height;

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Rectangle) {
            Rectangle rect = (Rectangle) obj;
            return super.equals(obj) && rect.x == x && rect.y == y && rect.width == width && rect.height == height;
        } else {
            return super.equals(obj);
        }
    }
}
