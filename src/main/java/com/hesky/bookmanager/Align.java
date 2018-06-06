package com.hesky.bookmanager;

/**
 * Represents HTML horizontal align attribute value
 */
public enum Align {

    ALIGN_RIGHT("right"), ALIGN_LEFT("left"), ALIGN_CENTER("center");
    private String align;

    Align(String align) {
        this.align = align;
    }

    @Override
    public String toString() {
        return align;
    }
}
