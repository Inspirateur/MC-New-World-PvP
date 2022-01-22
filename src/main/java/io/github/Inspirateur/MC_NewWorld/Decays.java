package io.github.Inspirateur.MC_NewWorld;
import java.util.ArrayList;
import java.util.HashMap;


public class Decays<K> {
    private final ArrayList<Decay<K>> decays;

    public Decays() {
        decays = new ArrayList<>();
    }

    public void addDecay(HashMap<K, Integer> decay, Decay.DecayCallback<K> callback) {
        decays.add(new Decay<>(decay, callback));
    }

    public void tick(int seconds) {
        for (Decay<K> decay : decays) {
            decay.tick(seconds);
        }
    }
}
