/*  Coast Dove
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package simonlang.coastdove.core.utility;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Constructs a power set of a given set element-by-element. The subsets are ordered by size,
 * increasing, e.g. for {A, B, C}, the values returned, in order, are: {C}, {B}, {A}, {C, B},
 * {C, A}, {B, A}, {C, B, A}. Note that the empty set { } is omitted.
 * @param <T> Type of elements contained in the original set (and thus, also in the sets of this
 *           powerset)
 */
public class PowerSet<T extends Comparable> {

//    public static int numberElementsForSize()

    private int setSize;
    private T[] setAsArray;
    private BitSet bitSet;
    private Comparator<? super T> comparator;

    /**
     * Creates a power set of the given set
     * @param set the original set
     * @param comparator the comparator to use when creating the subsets
     */
    //@SuppressWarnings("unchecked")
    public PowerSet(Set<T> set, Class<T[]> clazz, Comparator<? super T> comparator) {
        this.setSize = set.size();
        this.bitSet = new BitSet(setSize);
        this.comparator = comparator;
        if (setSize > 0) {
            this.setAsArray = set.toArray(clazz.cast(Array.newInstance(clazz.getComponentType(), setSize)));
            this.bitSet.set(setSize - 1);
        }
    }

    /**
     * Returns true if and only if the power set has another element
     * @return True iff the power set has another element
     */
    public boolean hasNext() {
        return bitSet.nextSetBit(0) != -1;
    }

    /**
     * Returns the size of the set returned when calling next()
     */
    public int sizeOfNext() {
        return bitSet.cardinality();
    }

    /**
     * Returns the size of the original set
     */
    public int getSetSize() {
        return setSize;
    }

    /**
     * Returns the next element. Can be called even if hasNext() returns false,
     * then the empty set is returned and the PowerSet is reset.
     * @return the next element
     */
    public Set<T> next() {
        Set<T> result = currentSubset();
        nextBitPattern();
        return result;
    }

    /**
     * @return the current subset that is returned by next()
     */
    private Set<T> currentSubset() {
        Set<T> result = new TreeSet<>(this.comparator);
        for (int index = bitSet.nextSetBit(0); index != -1; index = bitSet.nextSetBit(index + 1))
            result.add(setAsArray[index]);
        return result;
    }

    /**
     * Resets the power set to the point where next() returns the first
     * subset with #newCardinality elements
     * @param newCardinality the number of elements the next call of next() shall return
     */
    public void startOver(int newCardinality) {
        // 11100000
        // ^^^^^^^^
        // 00001111
        for (int i = 0; i < setSize; ++i)
            bitSet.clear(i);

        // only set new 1s if we haven't reached the maximum
        if (newCardinality <= setSize)
            for (int i = setSize - 1; i >= setSize - newCardinality; --i)
                bitSet.set(i);
    }

    /**
     * "Increments" the internal BitSet, i.e. moves the set values in a way that
     * all setups with n set values are used before going to (n+1) set values, e.g.
     * 0001
     * 0010
     * 0100
     * 1000
     * 0011
     * 0101
     * 1001
     * 0110
     * 1010
     * 1100
     * 0111
     * ...
     */
    private void nextBitPattern() {
        // not 111...10...000?
        if (bitSet.nextClearBit(0) < bitSet.previousSetBit(setSize)) {
            // 0...?
            if (bitSet.nextSetBit(0) > 0) {
                // 0010...11
                //   ^
                int nextSetIndex = bitSet.nextSetBit(0);

                // 0000...11
                bitSet.clear(nextSetIndex);

                // 0100...11
                bitSet.set(nextSetIndex - 1);
            }
            else {
                // 1110001...0
                // ^^^
                // 000
                int firstClearIndex = bitSet.nextClearBit(0);
                for (int i = 0; i < firstClearIndex; ++i)
                    bitSet.clear(i);

                // 0000001...0
                //   ^^^^^
                //   11110
                int nextSetIndex = bitSet.nextSetBit(firstClearIndex);
                bitSet.clear(nextSetIndex);
                int stopIndex = nextSetIndex - firstClearIndex;
                for (int i = nextSetIndex - 1; i >= stopIndex - 1; --i)
                    bitSet.set(i);
            }
        }
        else {
            startOver(bitSet.cardinality() + 1);
        }
    }

}
