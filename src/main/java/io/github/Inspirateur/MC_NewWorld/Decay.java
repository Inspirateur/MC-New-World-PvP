package io.github.Inspirateur.MC_NewWorld;
import java.util.HashMap;
import java.util.Iterator;


public class Decay<K> {
    interface DecayCallback<K> {
        void onKeyDecay(K key);
    }

    public Decay(HashMap<K, Integer> decay, DecayCallback<K> callback) {
        this.decay = decay;
        this.callback = callback;
    }

    HashMap<K, Integer> decay;
    DecayCallback<K> callback;

    public void tick(int seconds) {
        Iterator<HashMap.Entry<K, Integer>> it = decay.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<K, Integer> pair = it.next();
            int new_timer = pair.getValue()-seconds;
            if (new_timer < 0) {
                callback.onKeyDecay(pair.getKey());
                it.remove();
            } else {
                decay.put(pair.getKey(), new_timer);
            }
        }
    }
}