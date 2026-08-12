package com.example.helloworld;

public class MainActivity {

    public void onCreate() {
        System.out.println("Hello World");
    }

    public void onStart() {
        String message = "App started";
        showMessage(message);
    }

    private void showMessage(String msg) {
        System.out.println(msg);
    }
}