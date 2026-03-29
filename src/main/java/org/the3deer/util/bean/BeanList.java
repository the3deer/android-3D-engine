package org.the3deer.util.bean;

import androidx.annotation.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BeanList<T> extends AbstractList<T> {
    
    private final BeanFactory beanFactory;
    private final Class<T> elementType;
    private final String namedQualifier; // Optional: if lists can be qualified by @Named

    private List<T> list = new ArrayList<>();
    private boolean needsRefresh;

    // Constructor for general type
    public BeanList(BeanFactory bf, Class<T> type) {
        this(bf, type, null);
    }

    // Constructor with named qualifier
    public BeanList(BeanFactory bf, Class<T> type, String namedQualifier) {
        this.beanFactory = Objects.requireNonNull(bf);
        this.elementType = Objects.requireNonNull(type);
        this.namedQualifier = namedQualifier; // Can be null
        // Initial load can be done here or lazily on first access
    }

    // Called by BeanFactory when relevant changes occur
    public void markDirty() {
        this.needsRefresh = true;
    }

    private void ensureFresh() {
        if (needsRefresh) {
            // In a more complex scenario, findAll might need to know about the qualifier
            this.needsRefresh = false;

            List<T> newList = beanFactory.findAll(elementType, null);
            if (newList != null) { // findAll should ideally return empty list, not null
                this.list = newList;
            } else {
                this.list = Collections.emptyList();
            }
        }
    }

    @Override
    public boolean add(T element) {
        return this.list.add(element);
    }

    @Override
    public void add(int index, T element) {
        this.list.add(index, element);
    }

    @Override
    public T set(int index, T element) {
        return this.list.set(index, element);
    }

    @Override
    public T get(int index) {
        ensureFresh();
        return list.get(index);
    }

    @Override
    public T remove(int index) {
        return list.remove(index);
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return list.remove(o);
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return list.indexOf(o);
    }

    @Override
    public int size() {
        ensureFresh();
        return list.size();
    }

    // You MUST override iterator() and other methods you expect to be used,
    // ensuring they also call ensureFresh() or operate on the (now fresh) cachedList.
    // For example, iterator() should return an iterator over the fresh cachedList.
}