package ugent.waves.wearableapp;

import android.app.Application;

//ApplicationContext to make age available in the app
public class wearableAppApplication extends Application {
    private int age;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
