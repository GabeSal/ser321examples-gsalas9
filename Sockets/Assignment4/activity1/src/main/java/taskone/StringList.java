package taskone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StringList {

    private final List<String> strings = Collections.synchronizedList(new ArrayList<>());

    public void add(String str) {
        synchronized (strings) {
            if (!strings.contains(str)) {
                strings.add(str);
            }
        }
    }

    public List<String> getAll() {
        synchronized (strings) {
            return new ArrayList<>(strings);
        }
    }

    public int indexOf(String target) {
        synchronized (strings) {
            return strings.indexOf(target);
        }
    }

    public String reverseAt(int index) {
        synchronized (strings) {
            if (index < 0 || index >= strings.size()) {
                return null;
            }
            return new StringBuilder(strings.get(index)).reverse().toString();
        }
    }

    public boolean contains(String str) {
        synchronized (strings) {
            return strings.contains(str);
        }
    }

    public int size() {
        synchronized (strings) {
            return strings.size();
        }
    }

    @Override
    public String toString() {
        synchronized (strings) {
            return strings.toString();
        }
    }
}