package com.hesky.bookmanager;

public class DeskMgr {

    private static DeskMgr ourInstance = new DeskMgr();

    public static DeskMgr getInstance() {
        return ourInstance;
    }

    private DeskMgr() {
    }

    public static void main(String[] args) {
        System.out.println(getInstance());
        System.out.println(getInstance());
        System.out.println(getInstance());
    }
}
