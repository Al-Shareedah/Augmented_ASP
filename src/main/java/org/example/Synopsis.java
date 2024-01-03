package org.example;

import com.google.common.hash.HashFunction;

import java.util.HashSet;
import java.util.Set;

public class Synopsis {
    private Set<StreamingObject> objects;
    private HashFunction hashFunction;

    public Synopsis() {
        this.objects = new HashSet<>();

    }
    // Adds an object to the synopsis if its hash is within the smallest seen so far
    public void addObject(StreamingObject obj) {
        objects.add(obj);
    }

    // Removes an object from the synopsis
    public void removeObject(StreamingObject obj) {
        objects.remove(obj);
    }
    // Check if there are no objects in the synopsis
    public boolean isEmpty() {
        return objects.isEmpty();
    }


}
