package io.github.Inspirateur.MC_NewWorld;
import java.io.*;
import java.util.*;


public class Pacifists implements Serializable {
    private final String path = "plugins/MC_NewWorld/pacifists.ser";
    private Set<UUID> pacifists;

    public Pacifists() {
        load();
    }

    public void set(UUID pUID, boolean isPacifist) {
        if(isPacifist) {
            pacifists.add(pUID);
        } else {
            pacifists.remove(pUID);
        }
    }

    public boolean is(UUID pUID) {
        return pacifists.contains(pUID);
    }

    public void save() {
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this.pacifists);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        this.pacifists = new HashSet<>();
        if(new File(path).isFile()) {
            try {
                FileInputStream fileIn = new FileInputStream(path);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                //noinspection unchecked
                this.pacifists = (HashSet<UUID>) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException i) {
                i.printStackTrace();
            }
        } else {
            this.save();
        }
    }
}