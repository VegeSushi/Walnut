import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

public class Walnut extends MIDlet {
    private WalnutCanvas canvas;

    public Walnut() {
        canvas = new WalnutCanvas(this); //124, 164
    }

    protected void startApp() {
        Display.getDisplay(this).setCurrent(canvas);
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) {}
}