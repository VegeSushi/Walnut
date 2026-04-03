import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class WalnutCanvas extends Canvas {
    
    private Walnut midlet;
    private Image walnutImage;
    private Image enlargedWalnutImage;
    private int walnuts = 0;
    
    private boolean isPressed = false; 
    
    private static final String RECORD_STORE_NAME = "WalnutDB";

    public WalnutCanvas(Walnut midlet) {
        this.midlet = midlet;
        
        try {
            walnutImage = Image.createImage("/walnut.png");
            enlargedWalnutImage = scaleImage(walnutImage, 1.2); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        loadWalnuts();
    }

    protected void paint(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());

        Image imgToDraw = isPressed ? enlargedWalnutImage : walnutImage;
        g.drawImage(imgToDraw, getWidth() / 2, getHeight() / 2, Graphics.HCENTER | Graphics.VCENTER);

        g.setColor(0xFFFFFF);
        g.drawString("Walnuts: " + walnuts, getWidth() / 2, 10, Graphics.HCENTER | Graphics.TOP);
    }

    protected void keyPressed(int keyCode) {
        if (getGameAction(keyCode) == FIRE) {
            isPressed = true;
            walnuts++;
            saveWalnuts();
            repaint();
        }
    }

    protected void keyReleased(int keyCode) {
        if (getGameAction(keyCode) == FIRE) {
            isPressed = false;
            repaint();
        }
    }

    private Image scaleImage(Image src, double scaleFactor) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int dstW = (int) (srcW * scaleFactor);
        int dstH = (int) (srcH * scaleFactor);
        
        int[] srcPixels = new int[srcW * srcH];
        src.getRGB(srcPixels, 0, srcW, 0, 0, srcW, srcH);
        
        int[] dstPixels = new int[dstW * dstH];
        for (int y = 0; y < dstH; y++) {
            for (int x = 0; x < dstW; x++) {
                int srcX = (x * srcW) / dstW;
                int srcY = (y * srcH) / dstH;
                dstPixels[y * dstW + x] = srcPixels[srcY * srcW + srcX];
            }
        }
        
        return Image.createRGBImage(dstPixels, dstW, dstH, true);
    }

    private void loadWalnuts() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                
                walnuts = dis.readInt();
                
                dis.close();
                bais.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }

    private void saveWalnuts() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(walnuts);
            byte[] data = baos.toByteArray();
            
            dos.close();
            baos.close();

            if (rs.getNumRecords() > 0) {
                rs.setRecord(1, data, 0, data.length);
            } else {
                rs.addRecord(data, 0, data.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }
}