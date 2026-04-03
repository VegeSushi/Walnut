import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.rms.RecordStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.List;
import java.io.InputStream;

public class WalnutCanvas extends Canvas implements CommandListener, Runnable {
    
    private Walnut midlet;
    private Image walnutImage;
    private Image enlargedWalnutImage;
    
    private String[] itemNames;
    private int[] itemCosts;
    private int[] itemIncomes;
    private int[] itemCounts;
    private int totalStoreItems = 0;

    private boolean isPressed = false; 
    
    private long walnuts = 0;       
    private int upgrades = 0;       
    private long lastSaveTime = 0;  
    private long offlineEarnings = 0; 
    private volatile boolean isRunning = false; 
    
    private Command saveQuitCmd;
    private Command storeCmd;
    private Command backCmd;
    private List storeList;
    
    private static final String RECORD_STORE_NAME = "WalnutDB_v4";

    public WalnutCanvas(Walnut midlet) {
        this.midlet = midlet;
        
        try {
            walnutImage = Image.createImage("/walnut.png");
            enlargedWalnutImage = scaleImage(walnutImage, 1.2); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        loadStoreConfig();
        loadData();

        saveQuitCmd = new Command("Save & Quit", Command.EXIT, 1);
        storeCmd = new Command("Open Store", Command.ITEM, 2); 
        backCmd = new Command("Back", Command.BACK, 1);
        
        addCommand(saveQuitCmd);
        addCommand(storeCmd);
        setCommandListener(this);
        
        isRunning = true;
        new Thread(this).start();
    }

    public void commandAction(Command c, Displayable d) {
        if (d == this) { 
            if (c == saveQuitCmd) {
                isRunning = false; 
                saveData();
                midlet.notifyDestroyed();
            } else if (c == storeCmd) {
                openStore();
            }
        } else if (d == storeList) { 
            if (c == backCmd) {
                Display.getDisplay(midlet).setCurrent(this);
            } else if (c == List.SELECT_COMMAND) {
                buyItem(storeList.getSelectedIndex());
            }
        }
    }

    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {}
            
            if (upgrades > 0) {
                walnuts += upgrades;
                repaint();
            }
        }
    }

    protected void paint(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());

        Image imgToDraw = isPressed ? enlargedWalnutImage : walnutImage;
        g.drawImage(imgToDraw, getWidth() / 2, getHeight() / 2, Graphics.HCENTER | Graphics.VCENTER);

        g.setColor(0xFFFFFF);
        g.drawString("Walnuts: " + walnuts, getWidth() / 2, 10, Graphics.HCENTER | Graphics.TOP);

        g.setColor(0xAAAAAA); 
        g.drawString("Per Second: " + upgrades, getWidth() / 2, 30, Graphics.HCENTER | Graphics.TOP);
        
        if (offlineEarnings > 0) {
            g.setColor(0x00FF00); 
            g.drawString("Offline earnings: +" + offlineEarnings, getWidth() / 2, getHeight() - 20, Graphics.HCENTER | Graphics.BOTTOM);
        }
    }

    protected void keyPressed(int keyCode) {
        if (getGameAction(keyCode) == FIRE) {
            isPressed = true;
            walnuts++;
            offlineEarnings = 0;
            saveData();
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

    private void loadData() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                
                walnuts = dis.readLong();
                upgrades = dis.readInt();
                lastSaveTime = dis.readLong();
                
                if (dis.available() > 0) {
                    int savedItemsCount = dis.readInt();
                    for(int i = 0; i < savedItemsCount && i < totalStoreItems; i++) {
                        itemCounts[i] = dis.readInt();
                    }
                }
                
                dis.close();
                bais.close();
                
                if (lastSaveTime > 0 && upgrades > 0) {
                    long currentTime = System.currentTimeMillis();
                    long secondsPassed = (currentTime - lastSaveTime) / 1000;
                    
                    if (secondsPassed > 0) {
                        offlineEarnings = secondsPassed * upgrades;
                        walnuts += offlineEarnings;
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }

    private void saveData() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RECORD_STORE_NAME, true);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.writeLong(walnuts);
            dos.writeInt(upgrades);
            dos.writeLong(System.currentTimeMillis()); 
            
            dos.writeInt(totalStoreItems);
            for(int i = 0; i < totalStoreItems; i++) {
                dos.writeInt(itemCounts[i]);
            }
            
            byte[] data = baos.toByteArray();
            
            dos.close();
            baos.close();

            if (rs.getNumRecords() > 0) {
                rs.setRecord(1, data, 0, data.length);
            } else {
                rs.addRecord(data, 0, data.length);
            }
        } catch (Exception e) {
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }

    private int getCurrentCost(int index) {
        double cost = itemCosts[index];
        for (int i = 0; i < itemCounts[index]; i++) {
            cost *= 1.15;
        }
        return (int) cost;
    }

    private void openStore() {
        storeList = new List("Store (Walnuts: " + walnuts + ")", List.IMPLICIT);
        
        for(int i = 0; i < totalStoreItems; i++) {
            int currentCost = getCurrentCost(i);
            String label = itemNames[i] + " (" + itemCounts[i] + " owned) - " + currentCost + "W";
            storeList.append(label, null);
        }
        
        storeList.addCommand(backCmd);
        storeList.setCommandListener(this);
        Display.getDisplay(midlet).setCurrent(storeList);
    }

    private void buyItem(int index) {
        if (index >= 0 && index < totalStoreItems) {
            
            int currentCost = getCurrentCost(index);
            
            if (walnuts >= currentCost) {
                walnuts -= currentCost;
                upgrades += itemIncomes[index];
                itemCounts[index]++;
                
                saveData(); 
                offlineEarnings = 0; 
                
                int nextCost = getCurrentCost(index);
                
                String newLabel = itemNames[index] + " (" + itemCounts[index] + " owned) - " + nextCost + "W";
                storeList.set(index, newLabel, null);
                storeList.setTitle("Store (Walnuts: " + walnuts + ")");
            }
        }
    }

    private void loadStoreConfig() {
        try {
            InputStream is = getClass().getResourceAsStream("/store.txt");
            if (is == null) {
                System.out.println("Could not find store.txt!");
                return;
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
            String content = new String(baos.toByteArray());
            is.close();
            
            totalStoreItems = 0;
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) == '\n') totalStoreItems++;
            }
            if (!content.endsWith("\n")) totalStoreItems++;
            
            itemNames = new String[totalStoreItems];
            itemCosts = new int[totalStoreItems];
            itemIncomes = new int[totalStoreItems];
            itemCounts = new int[totalStoreItems];
            
            int itemIndex = 0;
            int lineStart = 0;
            for (int i = 0; i <= content.length(); i++) {
                if (i == content.length() || content.charAt(i) == '\n') {
                    String line = content.substring(lineStart, i).trim();
                    lineStart = i + 1;
                    
                    if (line.length() > 0) {
                        int comma1 = line.indexOf(',');
                        int comma2 = line.indexOf(',', comma1 + 1);
                        
                        if (comma1 != -1 && comma2 != -1) {
                            itemNames[itemIndex] = line.substring(0, comma1).trim();
                            itemCosts[itemIndex] = Integer.parseInt(line.substring(comma1 + 1, comma2).trim());
                            itemIncomes[itemIndex] = Integer.parseInt(line.substring(comma2 + 1).trim());
                            itemIndex++;
                        }
                    }
                }
            }
            totalStoreItems = itemIndex;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}