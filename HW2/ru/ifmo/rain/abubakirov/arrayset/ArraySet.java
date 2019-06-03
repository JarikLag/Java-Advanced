package ru.ifmo.rain.abubakirov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private List<E> mData;
    private Comparator<? super E> mComparator;

    public ArraySet() {
        mComparator = null;
        mData = Collections.emptyList();
    }

    public ArraySet(Collection<? extends E> collection) {
        mComparator = null;
        mData = new ArrayList<>(new TreeSet<>(collection));
    }

    public ArraySet(Comparator<? super E> comparator) {
        mComparator = comparator;
        mData = Collections.emptyList();
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        mComparator = comparator;
        SortedSet<E> temporarySet = new TreeSet<>(comparator);
        temporarySet.addAll(collection);
        mData = new ArrayList<>(temporarySet);
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(mData).iterator();
    }

    @Override
    public int size() {
        return mData.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return mComparator;
    }

    private boolean checkIndex(int index) {
        return index >= 0 && index < size();
    }

    private int findIndex(E element, int indicator) {
        int index = Collections.binarySearch(mData, element, mComparator);
        if (index < 0) {
            index = -index - 1;
        }
        if (checkIndex(index + indicator)) {
            return index + indicator;
        } else {
            return -1;
        }
    }

    private SortedSet<E> subSet(E fromElement, E toElement, int toInclusive) {
        int fromIndex = findIndex(fromElement, 0),
                toIndex = findIndex(toElement, toInclusive);
        if (fromIndex > toIndex || fromIndex == -1 || toIndex == -1) {
            return new ArraySet<>(mComparator);
        } else {
            return new ArraySet<>(mData.subList(fromIndex, toIndex + 1), mComparator);
        }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, toElement, -1);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        if (size() == 0) {
            return new ArraySet<>(mComparator);
        } else {
            return subSet(first(), toElement);
        }
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        if (size() == 0) {
            return new ArraySet<>(mComparator);
        } else {
            return subSet(fromElement, last(), 0);
        }
    }

    @Override
    public E first() {
        if (mData.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return mData.get(0);
        }
    }

    @Override
    public E last() {
        if (mData.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return mData.get(mData.size() - 1);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object object) {
        return Collections.binarySearch(mData, (E) object, mComparator) >= 0;
    }
}
