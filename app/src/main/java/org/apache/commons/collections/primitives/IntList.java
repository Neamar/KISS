/*
 * $Header: /home/cvs/jakarta-commons/primitives/src/java/org/apache/commons/collections/primitives/IntList.java,v 1.3 2003/10/16 20:49:36 scolebourne Exp $
 * ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.collections.primitives;

/**
 * An ordered collection of <code>int</code> values.
 *
 * @see org.apache.commons.collections.primitives.adapters.IntListList
 * @see org.apache.commons.collections.primitives.adapters.ListIntList
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 1.3 $ $Date: 2003/10/16 20:49:36 $
 * 
 * @author Rodney Waldhoff 
 */
public interface IntList extends IntCollection {
    /** 
     * Appends the specified element to the end of me
     * (optional operation).  Returns <code>true</code>
     * iff I changed as a result of this call.
     * <p/>
     * If a collection refuses to add the specified
     * element for any reason other than that it already contains
     * the element, it <i>must</i> throw an exception (rather than
     * simply returning <tt>false</tt>).  This preserves the invariant
     * that a collection always contains the specified element after 
     * this call returns. 
     * 
     * @param element the value whose presence within me is to be ensured
     * @return <code>true</code> iff I changed as a result of this call
     * 
     * @throws UnsupportedOperationException when this operation is not 
     *         supported
     * @throws IllegalArgumentException may be thrown if some aspect of the 
     *         specified element prevents it from being added to me
     */
    boolean add(int element);
       
    /** 
     * Inserts the specified element at the specified position 
     * (optional operation). Shifts the element currently 
     * at that position (if any) and any subsequent elements to the 
     * right, increasing their indices.
     * 
     * @param index the index at which to insert the element
     * @param element the value to insert
     * 
     * @throws UnsupportedOperationException when this operation is not 
     *         supported
     * @throws IllegalArgumentException if some aspect of the specified element 
     *         prevents it from being added to me
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    void add(int index, int element);
          
    /** 
     * Inserts all of the elements in the specified collection into me,
     * at the specified position (optional operation).  Shifts the 
     * element currently at that position (if any) and any subsequent 
     * elements to the right, increasing their indices.  The new elements 
     * will appear in the order that they are returned by the given 
     * collection's {@link IntCollection#iterator iterator}.
     * 
     * @param index the index at which to insert the first element from 
     *        the specified collection
     * @param collection the {@link IntCollection IntCollection} of elements to add 
     * @return <code>true</code> iff I changed as a result of this call
     * 
     * @throws UnsupportedOperationException when this operation is not 
     *         supported
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    boolean addAll(int index, IntCollection collection);
    
    /**
     * Returns <code>true</code> iff <i>that</i> is an <code>IntList</code>
     * that contains the same elements in the same order as me.
     * In other words, returns <code>true</code> iff <i>that</i> is
     * an <code>IntList</code> that has the same {@link #size size} as me,
     * and for which the elements returned by its 
     * {@link IntList#iterator iterator} are equal (<code>==</code>) to
     * the corresponding elements within me.
     * (This contract ensures that this method works properly across 
     * different implementations of the <code>IntList</code> interface.)
     * 
     * @param that the object to compare to me
     * @return <code>true</code> iff <i>that</i> is an <code>IntList</code>
     *         that contains the same elements in the same order as me
     */
    boolean equals(Object that);
    
    /** 
     * Returns the value of the element at the specified position 
     * within me. 
     * 
     * @param index the index of the element to return
     * @return the value of the element at the specified position
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    int get(int index);
        
    /**
     * Returns my hash code.
     * <p />
     * The hash code of an <code>IntList</code> is defined to be the
     * result of the following calculation:
     * <pre> int hash = 1;
     * for(IntIterator iter = iterator(); iter.hasNext(); ) {
     *   hash = 31*hash + iter.next();
     * }</pre>
     * <p />
     * This contract ensures that this method is consistent with 
     * {@link #equals equals} and with the 
     * {@link java.util.List#hashCode hashCode}
     * method of a {@link java.util.List List} of {@link Integer}s. 
     * 
     * @return my hash code
     */
    int hashCode();

    /** 
     * Returns the index of the first occurrence 
     * of the specified element within me, 
     * or <code>-1</code> if I do not contain 
     * the element. 
     * 
     * @param element the element to search for
     * @return the smallest index of an element matching the specified value,
     *         or <code>-1</code> if no such matching element can be found 
     */
    int indexOf(int element);
     
    /** 
     * Returns an {@link IntIterator iterator} over all my elements,
     * in the appropriate sequence.
     * @return an {@link IntIterator iterator} over all my elements.
     */
    IntIterator iterator();

    /** 
     * Returns the index of the last occurrence 
     * of the specified element within me, 
     * or -1 if I do not contain the element. 
     * 
     * @param element the element to search for
     * @return the largest index of an element matching the specified value,
     *         or <code>-1</code> if no such matching element can be found 
     */
    int lastIndexOf(int element);
    
    /** 
     * Returns a 
     * {@link IntListIterator bidirectional iterator}
     * over all my elements, in the appropriate sequence.
     */
    IntListIterator listIterator();
    
    /** 
     * Returns a 
     * {@link IntListIterator bidirectional iterator}
     * over all my elements, in the appropriate sequence, 
     * starting at the specified position. The 
     * specified <i>index</i> indicates the first 
     * element that would be returned by an initial 
     * call to the 
     * {@link IntListIterator#next next} 
     * method. An initial call to the 
     * {@link IntListIterator#previous previous}
     * method would return the element with the specified 
     * <i>index</i> minus one.
     * 
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    IntListIterator listIterator(int index);
    
    /** 
     * Removes the element at the specified position in 
     * (optional operation).  Any subsequent elements 
     * are shifted to the left, subtracting one from their 
     * indices.  Returns the element that was removed.
     * 
     * @param index the index of the element to remove
     * @return the value of the element that was removed
     * 
     * @throws UnsupportedOperationException when this operation is not 
     *         supported
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    int removeElementAt(int index);
   
    /** 
     * Replaces the element at the specified 
     * position in me with the specified element
     * (optional operation). 
     * 
     * @param index the index of the element to change
     * @param element the value to be stored at the specified position
     * @return the value previously stored at the specified position
     * 
     * @throws UnsupportedOperationException when this operation is not 
     *         supported
     * @throws IndexOutOfBoundsException if the specified index is out of range
     */
    int set(int index, int element);
    
    /** 
     * Returns a view of the elements within me 
     * between the specified <i>fromIndex</i>, inclusive, and 
     * <i>toIndex</i>, exclusive.  The returned <code>IntList</code>
     * is backed by me, so that any changes in 
     * the returned list are reflected in me, and vice-versa.
     * The returned list supports all of the optional operations
     * that I support.
     * <p/>
     * Note that when <code><i>fromIndex</i> == <i>toIndex</i></code>,
     * the returned list is initially empty, and when 
     * <code><i>fromIndex</i> == 0 && <i>toIndex</i> == {@link #size() size()}</code>
     * the returned list is my "improper" sublist, containing all my elements.
     * <p/>
     * The semantics of the returned list become undefined
     * if I am structurally modified in any way other than 
     * via the returned list.
     * 
     * @param fromIndex the smallest index (inclusive) in me that appears in 
     *        the returned list
     * @param toIndex the largest index (exclusive) in me that appears in the 
     *        returned list
     * @return a view of this list from <i>fromIndex</i> (inclusive) to 
     *         <i>toIndex</i> (exclusive)
     * 
     * @throws IndexOutOfBoundsException if either specified index is out of range
     */
    IntList subList(int fromIndex, int toIndex);

}
